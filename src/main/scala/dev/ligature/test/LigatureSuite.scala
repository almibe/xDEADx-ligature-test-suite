/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import dev.ligature._
import dev.ligature.Ligature.a
import munit._

abstract class LigatureSuite extends FunSuite {
  def createLigature: Ligature

  val testDataset: Dataset = Dataset("test")

  test("Create and close store") {
    val res = createLigature.instance.use { instance  =>
      instance.read.use { tx =>
        tx.datasets.compile.toList
      }
    }.unsafeRunSync()
    assert(res.isEmpty)
  }

  test("creating a new dataset") {
    val res = createLigature.instance.use { instance =>
      for {
        _ <- instance.write.use { tx =>
          tx.createDataset(testDataset)
        }
        res <- instance.read.use { tx =>
          tx.datasets.compile.toList
        }
      } yield res
    }.unsafeRunSync().toSet
    assertEquals(res, Set(testDataset))
  }

  test("access and delete new dataset") {
    val res = createLigature.instance.use { instance  =>
      for {
        _ <- instance.write.use { tx =>
          tx.createDataset(testDataset)
        }
        _ <- instance.write.use { tx =>
          for {
            _ <- tx.deleteDataset(testDataset)
            _ <- tx.deleteDataset(Dataset("test2"))
          } yield ()
        }
        res <- instance.read.use { tx =>
           tx.datasets.compile.toList
         }
      } yield res
    }.unsafeRunSync()
    assert(res.isEmpty)
  }

  test("new datasets should be empty") {
    val res = createLigature.instance.use { instance  =>
      for {
        _ <- instance.write.use { tx =>
          tx.createDataset(testDataset)
        }
        res <- instance.read.use { tx =>
          tx.allStatements(testDataset).compile.toList
        }
      } yield res
    }.unsafeRunSync()
    assert(res.isEmpty)
  }

  test("new node test") {
    val res = createLigature.instance.use { instance  =>
      for {
        _ <- instance.write.use { tx =>
          for {
            nn1 <- tx.newNode(testDataset)
            nn2 <- tx.newNode(testDataset)
            _   <- tx.addStatement(testDataset, Statement(nn1, a, nn2))
            nn3 <- tx.newNode(testDataset)
            nn4 <- tx.newNode(testDataset)
            _   <- tx.addStatement(testDataset, Statement(nn3, a, nn4))
          } yield ()
        }
        res <- instance.read.use { tx =>
          tx.allStatements(testDataset).compile.toList
        }
      } yield res
    }.unsafeRunSync().toSet
    assertEquals(res.map { _.statement }, Set(
      Statement(AnonymousNode(1), a, AnonymousNode(2)),
      Statement(AnonymousNode(4), a, AnonymousNode(5))))
  }

  test("adding statements to datasets") {
    val res = createLigature.instance.use { instance  =>
      for {
        _ <- instance.write.use { tx =>
          for {
            ent1 <- tx.newNode(testDataset)
            ent2 <- tx.newNode(testDataset)
            _    <- tx.addStatement(testDataset, Statement(ent1, a, ent2))
            _    <- tx.addStatement(testDataset, Statement(ent1, a, ent2))
          } yield()
        }
        res  <- instance.read.use { tx =>
          tx.allStatements(testDataset).map { _.statement }.compile.toList
        }
      } yield res
    }.unsafeRunSync().toSet
    assertEquals(res, Set(Statement(AnonymousNode(1), a, AnonymousNode(2)),
      Statement(AnonymousNode(1), a, AnonymousNode(2))))
  }

  test("removing statements from datasets") {
    val res = createLigature.instance.use { instance =>
      for {
        _ <- instance.write.use { tx =>
          for {
            nn1 <- tx.newNode(testDataset)
            nn2 <- tx.newNode(testDataset)
            nn3 <- tx.newNode(testDataset)
            _ <- tx.addStatement(testDataset, Statement(nn1, a, nn2))
            _ <- tx.addStatement(testDataset, Statement(nn3, a, nn2))
            _ <- tx.removeStatement(testDataset, Statement(nn1, a, nn2))
            _ <- tx.removeStatement(testDataset, Statement(nn1, a, nn2))
            _ <- tx.removeStatement(testDataset, Statement(nn2, a, nn1))
          } yield ()
        }
        res <- instance.read.use { tx =>
          tx.allStatements(testDataset).map {
            _.statement
          }.compile.toList
        }
      } yield res
    }.unsafeRunSync().toSet
    assertEquals(res, Set(Statement(AnonymousNode(3), a, AnonymousNode(2))))
  }

////  test("matching against a non-existent dataset") {
////    val res = createLigature.instance.use { instance  =>
////    val (r1, r2) = instance.read.use { tx =>
////      for {
////        r1 <- tx.matchStatements(testDataset, null, null, StringLiteral("French")).compile.toList
////        r2 <- tx.matchStatements(testDataset, null, a, null).compile.toList
////      } yield(r1, r2)
////    }
////  }
////
////  test("matching statements in datasets") {
////    val res = createLigature.instance.use { instance  =>
////    lateinit var valjean: Node
////    lateinit var javert: Node
////    instance.write.use { tx =>
////      valjean = tx.newNode(testDataset)
////      javert = tx.newNode(testDataset)
////      tx.addStatement(testDataset, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testDataset, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////      tx.addStatement(testDataset, Statement(javert, Predicate("nationality"), StringLiteral("French")))
////    }
////    instance.read.use { tx =>
////      tx.matchStatements(testDataset, null, null, StringLiteral("French"))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testDataset, null, null, LongLiteral(24601))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////      tx.matchStatements(testDataset, valjean)
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////      tx.matchStatements(testDataset, javert, Predicate("nationality"), StringLiteral("French"))
////              .toSet() shouldBe setOf(
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testDataset, null, null, null)
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////    }
////  }
////
////  test("matching statements with literals and ranges in datasets") {
////    val res = createLigature.instance.use { instance  =>
////    lateinit var valjean: Node
////    lateinit var javert: Node
////    lateinit var trout: Node
////    instance.write.use { tx =>
////      valjean = tx.newNode(testDataset)
////      javert = tx.newNode(testDataset)
////      trout = tx.newNode(testDataset)
////      tx.addStatement(testDataset, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testDataset, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////      tx.addStatement(testDataset, Statement(javert, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testDataset, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
////      tx.addStatement(testDataset, Statement(trout, Predicate("nationality"), StringLiteral("American")))
////      tx.addStatement(testDataset, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
////    }
////    instance.read.use { tx =>
////      tx.matchStatements(testDataset, null, null, StringLiteralRange("French", "German"))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testDataset, null, null, LongLiteralRange(24601, 24603))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
////                  Statement(javert, Predicate("prisonNumber"), LongLiteral(24602))
////      )
////      tx.matchStatements(testDataset, valjean, null, LongLiteralRange(24601, 24603))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////    }
////  }
////
////  test("matching statements with dataset literals in datasets") {
////    val res = createLigature.instance.use { instance  =>
////    val dataset = store.createDataset(NamedNode("test"))
////    dataset shouldNotBe null
////    val tx = dataset.writeTx()
////    TODO("Add values")
////    tx.commit()
////    val tx = dataset.tx()
////    TODO("Add assertions")
////    tx.cancel() // TODO add test running against a non-existant dataset w/ match-statement calls
////  }
}
