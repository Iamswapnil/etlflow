package etlflow.jobs

import etlflow.Schema.EtlJob1Props
import etlflow.TestSuiteHelper
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object EtlJobTestSuite extends DefaultRunnableSpec with TestSuiteHelper {

  def spec: ZSpec[environment.TestEnvironment, Any] =
    suite("EtlFlow")(
      suite("Etl Job 1")(
        testM("Execute Etl Job 1") {
          val job = EtlJob1Definition(EtlJob1Props(),Some(global_props))
          assertM(job.execute().foldM(ex => ZIO.fail(ex.getMessage), _ => ZIO.succeed("ok")))(equalTo("ok"))
        }
      )
    )
}


