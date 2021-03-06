package etlflow.etljobs

import etlflow.EtlJobProps
import etlflow.utils.GlobalProperties
import org.slf4j.{Logger, LoggerFactory}
import zio._

trait EtlJob {
  final val etl_job_logger: Logger = LoggerFactory.getLogger(getClass.getName)

  var job_name: String = getClass.getName
  val job_properties: EtlJobProps
  val global_properties: Option[GlobalProperties]
  val job_status: UIO[Ref[String]] = Ref.make("StatusNotSet")

  def printJobInfo(level: String = "info"): Unit
  def getJobInfo(level: String = "info"): List[(String,Map[String,String])]
  def execute(): ZIO[Any, Throwable, Unit]
}
