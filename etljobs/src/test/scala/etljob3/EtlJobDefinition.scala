package etljob3

import EtlJobSchemas.{Rating, RatingOutput}
import com.google.cloud.bigquery.BigQueryOptions
import org.apache.spark.sql.functions.{col, from_unixtime, input_file_name}
import org.apache.spark.sql.types.DateType
import org.apache.spark.sql.{Encoders, SparkSession, Dataset, SaveMode}
import etljobs.EtlJob
import etljobs.etlsteps.{BQLoadStep, EtlStep, SparkReadWriteStateStep}
import etljobs.etlsteps.SparkReadWriteStateStep.{Input, Output}
import etljobs.functions.SparkUDF
import etljobs.utils.{CSV, PARQUET, GlobalProperties, FSType}
import org.apache.log4j.{Level, Logger}
import etljobs.utils.SessionManager
import etljobs.spark.ReadApi

class EtlJobDefinition(job_properties: Map[String,String], global_properties: Option[GlobalProperties]) extends EtlJob(job_properties, global_properties) with SparkUDF {
  var output_date_paths : Seq[(String,String)] = Seq()
  val temp_date_col = "temp_date_col"
  Logger.getLogger("org").setLevel(Level.WARN)

  def enrichRatingData(spark: SparkSession, job_properties : Map[String, String])(in : Input[Rating,Unit]) : Output[RatingOutput,Unit] = {
    val mapping = Encoders.product[RatingOutput]

    val ratings_df = in.ds
        .withColumn("date", from_unixtime(col("timestamp"), "yyyy-MM-dd").cast(DateType))
        .withColumn(temp_date_col, get_formatted_date("date","yyyy-MM-dd","yyyyMMdd"))
        .where(f"$temp_date_col in ('20160101', '20160102')")

    import spark.implicits._

    output_date_paths = ratings_df
        .select(f"$temp_date_col")
        .distinct()
        .as[String]
        .collect()
        .map((date) => (job_properties("ratings_output_path") + f"/$temp_date_col=" + date + "/part*", date))

    ratings_df.drop(f"$temp_date_col")

    val ratings_ds = ratings_df.as[RatingOutput](mapping)

    Output[RatingOutput,Unit](ratings_ds,())
  }

  val step1 = SparkReadWriteStateStep[Rating, Unit, RatingOutput, Unit](
    name                    = "LoadRatingsParquet",
    input_location          = Seq(job_properties("ratings_input_path")),
    input_type              = CSV(",", true, job_properties.getOrElse("parse_mode","FAILFAST")),
    transform_function      = Some(enrichRatingData(spark, job_properties)),
    output_type             = PARQUET,
    output_location         = job_properties("ratings_output_path"),
    output_partition_col    = Some(f"$temp_date_col"),
    output_save_mode        = SaveMode.Overwrite,
    output_repartitioning   = true  // Setting this to true takes care of creating one file for every partition
  )(spark)

  val step2 = BQLoadStep(
    name                    = "LoadRatingBQ",
    source_paths_partitions = output_date_paths,
    source_format           = PARQUET,
    destination_dataset     = job_properties("ratings_output_dataset"),
    destination_table       = job_properties("ratings_output_table_name")
  )(bq)

  def apply() : List[EtlStep[Unit,Unit]] = List(step1,step2)
}
