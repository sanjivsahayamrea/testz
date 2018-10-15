/*
 * Copyright (c) 2018, Edmund Noble
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package testz

import runner.TestOutput

import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.global
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    // The "default" way to print test results:
    // use `runner.printTest` for formatting and
    // `runner.printStrs` with `Console.print`
    // for printing.
    val printer: (Result, List[String]) => Unit =
      (tr, ls) => runner.printStrs(runner.printTest(tr, ls), Console.print)

    // Not always a good choice; parallelism slows down heavily contended machines, for example.
    val ec = global

    val test = PureHarness.test(printer)

    val section = PureHarness.section

    val futureTest = FutureHarness.test(printer)(ec)

    val futureSection = FutureHarness.section(ec)

    def unitTests = TestOutput.combineAll1(
      ExtrasSuite.tests(test, section)((), List("Extras tests")),
      PropertySuite.tests(test, section)((), List("Property tests")),
      StdlibSuite.tests(test, section, ec)((), List("Stdlib tests")),
      CoreSuite.tests(test, section)((), List("Core tests")),
      ScalazSuite.tests(test, section)((), List("Scalaz tests")),
    )

    def propertyTests =
      PropertySuite.tests(test, section)((), List("Property tests"))

    def runnerTests =
      RunnerSuite.tests(futureTest, futureSection, ec)((), List("Runner tests"))

    // Evaluate tests before the runner expects,
    // for parallelism.
    val testOutputs: List[() => Future[TestOutput]] = List(
      Future(unitTests)(ec),
      Future(propertyTests)(ec),
      Future(runnerTests)(ec).flatMap(x => x)(ec)
    ).map(s => () => s)

    val runSuites = runner(testOutputs, Console.print(_), global)
    val result = Await.result(runSuites, Duration.Inf)

    if (result.failed) throw new Exception("some tests failed")
  }
}
