/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import dev.ligature._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.Runtime

abstract class LigatureSuite extends AnyFlatSpec with Matchers {
  val runtime: Runtime[zio.ZEnv] = Runtime.default
  def createSession(): LigatureSession

  val testCollection: NamedEntity = NamedEntity("test")

  it should "Create and close store" in {
    val store = createSession()
    val c = store.compute.use( tx => tx.collections )
    runtime.unsafeRun(c).toSet shouldBe Set()
  }

  it should "creating a new collection" in {
    val store = createSession()

    runtime.unsafeRun(store.write.use { tx =>
      tx.createCollection(testCollection)
    })

    val c = store.compute.use { tx =>
      tx.collections
    }

    runtime.unsafeRun(c).toSet shouldBe Set(testCollection)
  }

//  it should "access and delete new collection" in {
//    val store = createSession()
//
//    store.write.use( tx => for {
//      x <- tx.createCollection(testCollection)
//    } yield x).unsafeRunSync()
//
//    val c = store.compute.use( tx => for {
//      c <- tx.collections
//    } yield c)
//
//    c.unsafeRunSync().toSet shouldBe Set(testCollection)
//
//    store.write.use( tx => for {
//      x <- tx.deleteCollection(testCollection)
//      _ <- tx.deleteCollection(NamedEntity("Test2"))
//    } yield x).unsafeRunSync()
//
//    val c2 = store.compute.use( tx => for {
//      c <- tx.collections
//    } yield c)
//
//    c2.unsafeRunSync().toSet shouldBe Set()
//  }
//
//  it should "new collections should be empty" in {
//    val store = createSession()
//
//    store.write.use( tx => for {
//      x <- tx.createCollection(testCollection)
//    } yield x).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().toSet shouldBe Set()
//
//  }
//
//  it should "adding statements to collections" in {
//    val store = createSession()
//
//    store.write.use( tx => for {
//      _ <- tx.addStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human")))
//      r <- tx.addStatement(testCollection, Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
//    } yield r).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
//      Set(Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human")),
//            Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
//
//  }
//
////  it should "removing statements from collections" in {
////    val store = createSession()
////
////    store.write.use( tx => for {
////      _ <- tx.addStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human")))
////      _ <- tx.addStatement(testCollection, Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
////      x <- tx.removeStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human")))
////    } yield x).unsafeRunSync()
////
////    val s = store.compute.use( tx => for {
////      s <- tx.allStatements(testCollection)
////    } yield s)
////
////    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
////      Set(Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
////
////  }
//
//  it should "new entity test" in {
//    val store = createSession()
//
//    store.write.use( tx => for {
//      e0 <- tx.newEntity(testCollection)
//      e1 <- tx.newEntity(testCollection)
//      e2 <- tx.newEntity(testCollection)
//      e3 <- tx.newEntity(testCollection)
//      _ <- tx.addStatement(testCollection, Statement(e0.get, Ligature.a, e1.get))
//      _ <- tx.addStatement(testCollection, Statement(e2.get, Ligature.a, e3.get))
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(AnonymousEntity(0), Ligature.a, AnonymousEntity(1)),
//      Statement(AnonymousEntity(2), Ligature.a, AnonymousEntity(3)))
//
//  }
//
////  it should "removing named entity" in {
////    val store = createSession()
////    val entA = NamedEntity("a")
////    val entB = NamedEntity("b")
////    val entC = NamedEntity("c")
////
////    store.write.use( tx => for {
////      _ <- tx.addStatement(testCollection, Statement(entA, Ligature.a, entB))
////      _ <- tx.addStatement(testCollection, Statement(entC, Predicate("a"), entB))
////      _ <- tx.addStatement(testCollection, Statement(entB, Ligature.a, entA))
////      _ <- tx.removeEntity(testCollection, entA)
////    } yield ()).unsafeRunSync()
////
////    val s = store.compute.use( tx => for {
////      s <- tx.allStatements(testCollection)
////    } yield s)
////
////    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
////      Set(Statement(NamedEntity("c"), Predicate("a"), NamedEntity("b")))
////
////  }
////
////  it should "removing anonymous entity" in {
////    val store = createSession()
////
////    store.write.use( tx => for {
////      ent1 <- tx.newEntity(testCollection)
////      ent2 <- tx.newEntity(testCollection)
////      ent3 <- tx.newEntity(testCollection)
////      _ <- tx.addStatement(testCollection, Statement(ent1.get, Ligature.a, ent2.get))
////      _ <- tx.addStatement(testCollection, Statement(ent3.get, Ligature.a, ent2.get))
////      _ <- tx.addStatement(testCollection, Statement(ent2.get, Ligature.a, ent1.get))
////      _ <- tx.removeEntity(testCollection, ent1.get)
////    } yield ()).unsafeRunSync()
////
////    val s = store.compute.use( tx => for {
////      s <- tx.allStatements(testCollection)
////    } yield s)
////
////    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
////      Set(Statement(AnonymousEntity(3), Ligature.a, AnonymousEntity(2)))
////
////  }
////
////  it should "removing predicate" in {
////    val store = createSession()
////    val namedA = NamedEntity(Ligature.a.identifier)
////    store.write.use( tx => for {
////      ent1 <- tx.newEntity(testCollection)
////      _ <- tx.addStatement(testCollection, Statement(ent1.get, Ligature.a, NamedEntity("test")))
////      _ <- tx.addStatement(testCollection, Statement(namedA, Predicate("test"), namedA))
////      _ <- tx.addStatement(testCollection, Statement(namedA, Ligature.a, ent1.get))
////      _ <- tx.removePredicate(testCollection, Ligature.a)
////    } yield ()).unsafeRunSync()
////
////    val s = store.compute.use( tx => for {
////      s <- tx.allStatements(testCollection)
////    } yield s)
////
////    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
////      Set(Statement(namedA, Predicate("test"), namedA))
////
////  }
//
//  it should "matching against a non-existant collection" in {
//    val store = createSession()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.matchStatements(testCollection, None, None, Some(StringLiteral("French")))
//      s2 <- tx.matchStatements(testCollection, None, Some(Ligature.a), None)
//    } yield (s, s2))
//
//    s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set()
//    s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set()
//
//  }
//
//  it should "matching statements in collections" in {
//      val store = createSession()
//      val valjean = NamedEntity("valjean")
//      val javert = NamedEntity("javert")
//
//      store.write.use( tx => for {
//        _ <- tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
//        _ <- tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
//        _ <- tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
//      } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.matchStatements(testCollection, None, None, Some(StringLiteral("French")))
//      s2 <- tx.matchStatements(testCollection, None, None, Some(LongLiteral(24601)))
//      s3 <- tx.matchStatements(testCollection, Some(valjean))
//      s4 <- tx.matchStatements(testCollection, Some(javert),
//        Some(Predicate("nationality")),
//        Some(StringLiteral("French")))
//      s5 <- tx.matchStatements(testCollection, None, None, None)
//    } yield (s, s2, s3, s4, s5))
//
//    s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//      Statement(javert, Predicate("nationality"), StringLiteral("French")))
//    s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
//    s.unsafeRunSync()._3.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//      Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
//    s.unsafeRunSync()._4.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(javert, Predicate("nationality"), StringLiteral("French")))
//    s.unsafeRunSync()._5.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//      Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
//      Statement(javert, Predicate("nationality"), StringLiteral("French")))
//
//
//  }
//
////  it should "matching statements with literals and ranges in collections" in {
////    val store = createSession()
////    val valjean = NamedEntity("valjean")
////    val javert = NamedEntity("javert")
////    val trout = NamedEntity("trout")
////
////    store.write.use( tx => for {
////      _ <- tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
////      _ <- tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////      _ <- tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
////      _ <- tx.addStatement(testCollection, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
////      _ <- tx.addStatement(testCollection, Statement(trout, Predicate("nationality"), StringLiteral("American")))
////      _ <- tx.addStatement(testCollection, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
////    } yield ()).unsafeRunSync()
////
////    val s = store.compute.use( tx => for {
////      s <- tx.matchStatements(testCollection, None, None, Range(StringLiteral("French"), StringLiteral("German")))
////      s2 <- tx.matchStatements(testCollection, None, None, Range(LongLiteral(24601), LongLiteral(24603)))
////      s3 <- tx.matchStatements(testCollection, Some(valjean), None, Range(LongLiteral(24601), LongLiteral(24603)))
////    } yield (s, s2, s3))
////
////    s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
////      Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////      Statement(javert, Predicate("nationality"), StringLiteral("French")))
////    s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
////      Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
////      Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
////    s.unsafeRunSync()._3.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
////      Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////
////
////  }
}
