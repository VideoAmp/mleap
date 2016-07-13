package org.apache.spark.ml.mleap.converter

import com.truecar.mleap.runtime.types.StringArrayType
import com.truecar.mleap.spark
import com.truecar.mleap.spark.SparkDataset
import com.truecar.mleap.runtime.types
import com.truecar.mleap.spark.SparkLeapFrame
import org.apache.spark.ml.mleap
import org.apache.spark.ml.linalg.{Vector, VectorUDT}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types._
import com.truecar.mleap.runtime.{Row => MleapRow}

/**
  * Created by hwilkins on 11/18/15.
  */
case class DataFrameToMleap(dataset: DataFrame) {
  def toMleap: SparkLeapFrame = {
    val mleapFields = dataset.schema.fields.flatMap {
      field =>
        field.dataType match {
          case _: NumericType | BooleanType => Seq(types.StructField(field.name, types.DoubleType))
          case _: VectorUDT => Seq(types.StructField(field.name, types.VectorType))
          case _: StringType => Seq(types.StructField(field.name, types.StringType))
          case dataType: ArrayType =>
            dataType.elementType match {
              case StringType => Seq(types.StructField(field.name, StringArrayType))
              case _ => Seq()
            }
          case _ => Seq()
        }
    }

    toMleap(types.StructType(mleapFields))
  }

  def toMleap(schema: types.StructType): SparkLeapFrame = {
    val sparkSchema = dataset.schema

    // cast MLeap field numeric types to DoubleTypes
    val mleapCols = schema.fields.map {
      field =>
        field.dataType match {
          case types.DoubleType => dataset.col(field.name).cast(DoubleType).as(s"mleap.${field.name}")
          case types.StringType => dataset.col(field.name).cast(StringType).as(s"mleap.${field.name}")
          case types.VectorType => dataset.col(field.name).as(s"mleap.${field.name}")
          case types.StringArrayType => dataset.col(field.name).cast(new ArrayType(StringType, containsNull = false)).as(s"mleap.${field.name}")
        }
    }
    val cols = Seq(dataset.col("*")) ++ mleapCols
    val castDataset = dataset.select(cols: _*)

    val sparkIndices = sparkSchema.fields.indices
    val mleapIndices = sparkSchema.fields.length until (sparkSchema.fields.length + schema.fields.length)

    val rdd = castDataset.rdd.map {
      row =>
        val mleapValues = mleapIndices.map {
          index => row.get(index) match {
            case vector: Vector => VectorToMleap(vector).toMleap
            case value => value
          }
        }
        val mleapRow = MleapRow(mleapValues: _*)
        val sparkValues: IndexedSeq[Any] = sparkIndices.map(row.get)

        (mleapRow, sparkValues)
    }

    val mleapDataset = SparkDataset(rdd)
    SparkLeapFrame(schema,
      sparkSchema,
      mleapDataset)
  }
}
