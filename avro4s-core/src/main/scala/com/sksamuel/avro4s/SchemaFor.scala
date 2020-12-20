package com.sksamuel.avro4s

import com.sksamuel.avro4s.schemas.{BaseSchemas, CollectionSchemas, TupleSchemas}

import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.{Instant, LocalDate, LocalDateTime, OffsetDateTime}
import java.util
import java.util.{Date, UUID}
import org.apache.avro.util.Utf8
import org.apache.avro.{LogicalType, LogicalTypes, Schema, SchemaBuilder}

import scala.deriving.Mirror

object AvroSchema {
  def apply[T](using schemaFor: SchemaFor[T]): Schema = schemaFor.schema[T]
}

/**
 * A [[SchemaFor]] generates an Avro Schema for a Scala or Java type.
 *
 * For example, a SchemaFor[String] could return a schema of type Schema.Type.STRING, and
 * a SchemaFor[Int] could return an schema of type Schema.Type.INT
 */
trait SchemaFor[T]:

  /**
   * Returns the avro [Schema] generated by this typeclass.
   */
  def schema[T]: Schema

  /**
   * Changes the type of this SchemaFor to the desired type `U` without any other modifications.
   *
   * @tparam U new type for SchemaFor.
   */
  def forType[U]: SchemaFor[U] = map[U](identity)

  /**
   * Creates a SchemaFor[U] by applying a function Schema => Schema
   * to the schema generated by this instance.
   */
  def map[U](fn: Schema => Schema): SchemaFor[U] = {
    val self = this
    return new SchemaFor[U] {
      override def schema[U] = fn(self.schema)
    }
  }

object SchemaFor extends BaseSchemas with ByteIterableSchemas with CollectionSchemas with TupleSchemas {

  def apply[T](s: Schema): SchemaFor[T] = new SchemaFor[T] {
    override def schema[T]: Schema = s
  }

  inline given derived[T](using m: Mirror.Of[T]) : SchemaFor[T] = SchemaForMacros.derive[T]

  //  inline def schemaForProduct[T](p: Mirror.Product, recordName: String, elems: List[SchemaFor[_]], labels: List[String]): SchemaFor[T] = {
  //
  //    val fields = new util.ArrayList[Schema.Field]()
  //    elems.zip(labels).foreach { case (fieldSchemaFor, fieldName) =>
  //      val field = new Schema.Field(fieldName, fieldSchemaFor.schema, null)
  //      fields.add(field)
  //    }
  //    // todo use schema builder once cyclic reference bug in dotty is fixed
  //    val _schema = Schema.createRecord(recordName, null, "mynamespace", false, fields)
  //    new SchemaFor[T] :
  //      override def schema[T]: Schema = _schema
  //  }

  //  inline def labelsToList[T <: Tuple]: List[String] =
  //    inline erasedValue[T] match {
  //      case _: Unit => Nil
  //      case _: (head *: tail) => (inline constValue[head] match {
  //        case str: String => str
  //        case other => other.toString
  //      }) :: labelsToList[tail]
  //      // todo why is this Any required, why doesn't Unit grab the empty type?
  //      case _: Any => Nil
  //    }
  //
  //  inline def summonAll[T]: List[SchemaFor[_]] = inline erasedValue[T] match {
  //    case _: EmptyTuple => Nil
  //    case _: (t *: ts) => summonInline[SchemaFor[t]] :: summonAll[ts]
  //  }
  //
  //  inline given derived[T](using m: Mirror.Of[T]) as SchemaFor[T] = {
  //    val elemInstances = summonAll[m.MirroredElemTypes]
  //    val labels = labelsToList[m.MirroredElemLabels]
  //    val name = inline constValue[m.MirroredLabel] match {
  //      case str: String => str
  //    }
  //    inline m match {
  //      case s: Mirror.SumOf[T] => ???
  //      case p: Mirror.ProductOf[T] => schemaForProduct(p, name, elemInstances, labels)
  //    }
  //  }
}



object TimestampNanosLogicalType extends LogicalType("timestamp-nanos") {
  override def validate(schema: Schema): Unit = {
    super.validate(schema)
    if (schema.getType != Schema.Type.LONG) {
      throw new IllegalArgumentException("Logical type timestamp-nanos must be backed by long")
    }
  }
}

object OffsetDateTimeLogicalType extends LogicalType("datetime-with-offset") {
  override def validate(schema: Schema): Unit = {
    super.validate(schema)
    if (schema.getType != Schema.Type.STRING) {
      throw new IllegalArgumentException("Logical type iso-datetime with offset must be backed by String")
    }
  }
}


trait ByteIterableSchemas:
  given ByteArraySchemaFor: SchemaFor[Array[Byte]] = SchemaFor[Array[Byte]](SchemaBuilder.builder.bytesType)
  given ByteListSchemaFor: SchemaFor[List[Byte]] = ByteArraySchemaFor.forType
  given ByteSeqSchemaFor: SchemaFor[Seq[Byte]] = ByteArraySchemaFor.forType
  given ByteVectorSchemaFor: SchemaFor[Vector[Byte]] = ByteArraySchemaFor.forType

