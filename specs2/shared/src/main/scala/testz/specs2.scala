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

import scala.concurrent.Future

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable
import org.specs2.specification.core.Fragment

object specs2 {
  object Specs2Harness {
    def testFromSpec(spec: mutable.Specification, ee: ExecutionEnv): Test[Future[Result], () => Fragment] = {
      implicit val exEnv = ee
      import spec._
      new Test[Future[Result], () => Fragment] {
        def apply
          (name: String)
          (assertion: () => Future[Result])
          : () => Fragment = () =>
          // todo: catch exceptions?
          name in {
            assertion().map(_ must_== Succeed).await
          }
      }
    }

    def sectionFromSpec(spec: mutable.Specification): Section[() => Fragment] =
      new Section[() => Fragment] {
        import spec._

        def named(name: String)(
          t1: () => Fragment, ts: () => Fragment*
        ): () => Fragment = { () =>
          name should {
            val h = t1()
            ts.map(_()).lastOption.getOrElse(h)
          }
        }

        def apply(
          t1: () => Fragment, ts: () => Fragment*
        ): () => Fragment = { () =>
          val h = t1()
          ts.map(_()).lastOption.getOrElse(h)
        }
      }
  }
}
