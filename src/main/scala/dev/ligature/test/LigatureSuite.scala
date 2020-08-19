/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import cats.effect.{IO, Resource}
import dev.ligature._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class LigatureSuite extends AnyFlatSpec with Matchers {
  def createSession(): Resource[IO, LigatureSession]

  val testCollection: NamedElement = NamedElement("test")

  it should "Create and close store" in {
    createSession().use { store => IO {
      val c = store.compute.use { tx => {
        tx.collections
      } }.unsafeRunSync()
      c.toSet shouldBe Set()
    }}.unsafeRunSync()
  }

  it should "creating a new collection" in {
    createSession().use { store => IO {
      store.write.use { tx =>
        tx.createCollection(testCollection)
      }.unsafeRunSync()

      val c = store.compute.use { tx =>
        tx.collections
      }.unsafeRunSync()

      c.toSet shouldBe Set(testCollection)
    }}.unsafeRunSync()
  }

  it should "access and delete new collection" in {
    createSession().use { store => IO {
      store.write.use(tx => for {
        x <- tx.createCollection(testCollection)
      } yield x).unsafeRunSync()

      val c = store.compute.use(tx => for {
        c <- tx.collections
      } yield c)

      c.unsafeRunSync().toSet shouldBe Set(testCollection)

      store.write.use(tx => for {
        x <- tx.deleteCollection(testCollection)
        _ <- tx.deleteCollection(NamedElement("Test2"))
      } yield x).unsafeRunSync()

      val c2 = store.compute.use(tx => for {
        c <- tx.collections
      } yield c)

      c2.unsafeRunSync().toSet shouldBe Set()
    }}.unsafeRunSync()
  }

  it should "new collections should be empty" in {
    createSession().use { store =>
      IO {
        store.write.use(tx => for {
          x <- tx.createCollection(testCollection)
        } yield x).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.allStatements(testCollection)
        } yield s)

        s.unsafeRunSync().toSet shouldBe Set()
      }
    }.unsafeRunSync()
  }

  it should "adding statements to collections" in {
    createSession().use { store =>
      IO {
        store.write.use(tx => for {
          _ <- tx.addStatement(testCollection, Statement(NamedElement("Alex"), Ligature.a, NamedElement("Human")))
          r <- tx.addStatement(testCollection, Statement(NamedElement("Clarice"), Ligature.a, NamedElement("Feline")))
        } yield r).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.allStatements(testCollection)
        } yield s)

        s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
          Set(Statement(NamedElement("Alex"), Ligature.a, NamedElement("Human")),
            Statement(NamedElement("Clarice"), Ligature.a, NamedElement("Feline")))

      }
    }.unsafeRunSync()
  }

//  it should "removing statements from collections" in {
//    createSession().use { store => IO {
//
//    store.write.use( tx => for {
//      _ <- tx.addStatement(testCollection, Statement(NamedElement("Alex"), Ligature.a, NamedElement("Human")))
//      _ <- tx.addStatement(testCollection, Statement(NamedElement("Clarice"), Ligature.a, NamedElement("Feline")))
//      x <- tx.removeStatement(testCollection, Statement(NamedElement("Alex"), Ligature.a, NamedElement("Human")))
//    } yield x).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
//      Set(Statement(NamedElement("Clarice"), Ligature.a, NamedElement("Feline")))
//
//  }

  it should "new entity test" in {
    createSession().use { store =>
      IO {

        store.write.use(tx => for {
          e0 <- tx.newEntity(testCollection)
          e1 <- tx.newEntity(testCollection)
          e2 <- tx.newEntity(testCollection)
          e3 <- tx.newEntity(testCollection)
          _ <- tx.addStatement(testCollection, Statement(e0.get, Ligature.a, e1.get))
          _ <- tx.addStatement(testCollection, Statement(e2.get, Ligature.a, e3.get))
        } yield ()).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.allStatements(testCollection)
        } yield s)

        s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(AnonymousEntity(0), Ligature.a, AnonymousEntity(1)),
          Statement(AnonymousEntity(2), Ligature.a, AnonymousEntity(3)))
      }
    }.unsafeRunSync()
  }

//  it should "removing named entity" in {
//    createSession().use { store => IO {
//    val entA = NamedElement("a")
//    val entB = NamedElement("b")
//    val entC = NamedElement("c")
//
//    store.write.use( tx => for {
//      _ <- tx.addStatement(testCollection, Statement(entA, Ligature.a, entB))
//      _ <- tx.addStatement(testCollection, Statement(entC, NamedElement("a"), entB))
//      _ <- tx.addStatement(testCollection, Statement(entB, Ligature.a, entA))
//      _ <- tx.removeEntity(testCollection, entA)
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
//      Set(Statement(NamedElement("c"), NamedElement("a"), NamedElement("b")))
//
//  }
//
//  it should "removing anonymous entity" in {
//    createSession().use { store => IO {
//
//    store.write.use( tx => for {
//      ent1 <- tx.newEntity(testCollection)
//      ent2 <- tx.newEntity(testCollection)
//      ent3 <- tx.newEntity(testCollection)
//      _ <- tx.addStatement(testCollection, Statement(ent1.get, Ligature.a, ent2.get))
//      _ <- tx.addStatement(testCollection, Statement(ent3.get, Ligature.a, ent2.get))
//      _ <- tx.addStatement(testCollection, Statement(ent2.get, Ligature.a, ent1.get))
//      _ <- tx.removeEntity(testCollection, ent1.get)
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
//      Set(Statement(AnonymousEntity(3), Ligature.a, AnonymousEntity(2)))
//
//  }
//
//  it should "removing NamedElement" in {
//    createSession().use { store => IO {
//    val namedA = NamedElement(Ligature.a.identifier)
//    store.write.use( tx => for {
//      ent1 <- tx.newEntity(testCollection)
//      _ <- tx.addStatement(testCollection, Statement(ent1.get, Ligature.a, NamedElement("test")))
//      _ <- tx.addStatement(testCollection, Statement(namedA, NamedElement("test"), namedA))
//      _ <- tx.addStatement(testCollection, Statement(namedA, Ligature.a, ent1.get))
//      _ <- tx.removeNamedElement(testCollection, Ligature.a)
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allStatements(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ps: PersistedStatement) => ps.statement).toSet shouldBe
//      Set(Statement(namedA, NamedElement("test"), namedA))
//
//  }

  it should "matching against a non-existant collection" in {
    createSession().use { store =>
      IO {
        val s = store.compute.use(tx => for {
          s <- tx.matchStatements(testCollection, None, None, Some(StringLiteral("French")))
          s2 <- tx.matchStatements(testCollection, None, Some(Ligature.a), None)
        } yield (s, s2))

        s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set()
        s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set()

      }
    }.unsafeRunSync()
  }

  it should "matching statements in collections" in {
    createSession().use { store =>
      IO {
        val valjean = NamedElement("valjean")
        val javert = NamedElement("javert")

        store.write.use(tx => for {
          _ <- tx.addStatement(testCollection, Statement(valjean, NamedElement("nationality"), StringLiteral("French")))
          _ <- tx.addStatement(testCollection, Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)))
          _ <- tx.addStatement(testCollection, Statement(javert, NamedElement("nationality"), StringLiteral("French")))
        } yield ()).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.matchStatements(testCollection, None, None, Some(StringLiteral("French")))
          s2 <- tx.matchStatements(testCollection, None, None, Some(LongLiteral(24601)))
          s3 <- tx.matchStatements(testCollection, Some(valjean))
          s4 <- tx.matchStatements(testCollection, Some(javert),
            Some(NamedElement("nationality")),
            Some(StringLiteral("French")))
          s5 <- tx.matchStatements(testCollection, None, None, None)
        } yield (s, s2, s3, s4, s5))

        s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(valjean, NamedElement("nationality"), StringLiteral("French")),
          Statement(javert, NamedElement("nationality"), StringLiteral("French")))
        s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)))
        s.unsafeRunSync()._3.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(valjean, NamedElement("nationality"), StringLiteral("French")),
          Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)))
        s.unsafeRunSync()._4.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(javert, NamedElement("nationality"), StringLiteral("French")))
        s.unsafeRunSync()._5.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
          Statement(valjean, NamedElement("nationality"), StringLiteral("French")),
          Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)),
          Statement(javert, NamedElement("nationality"), StringLiteral("French")))
      }
    }.unsafeRunSync()
  }

//  it should "matching statements with literals and ranges in collections" in {
//    createSession().use { store => IO {
//    val valjean = NamedElement("valjean")
//    val javert = NamedElement("javert")
//    val trout = NamedElement("trout")
//
//    store.write.use( tx => for {
//      _ <- tx.addStatement(testCollection, Statement(valjean, NamedElement("nationality"), StringLiteral("French")))
//      _ <- tx.addStatement(testCollection, Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)))
//      _ <- tx.addStatement(testCollection, Statement(javert, NamedElement("nationality"), StringLiteral("French")))
//      _ <- tx.addStatement(testCollection, Statement(javert, NamedElement("prisonNumber"), LongLiteral(24602)))
//      _ <- tx.addStatement(testCollection, Statement(trout, NamedElement("nationality"), StringLiteral("American")))
//      _ <- tx.addStatement(testCollection, Statement(trout, NamedElement("prisonNumber"), LongLiteral(24603)))
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.matchStatements(testCollection, None, None, Range(StringLiteral("French"), StringLiteral("German")))
//      s2 <- tx.matchStatements(testCollection, None, None, Range(LongLiteral(24601), LongLiteral(24603)))
//      s3 <- tx.matchStatements(testCollection, Some(valjean), None, Range(LongLiteral(24601), LongLiteral(24603)))
//    } yield (s, s2, s3))
//
//    s.unsafeRunSync()._1.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, NamedElement("nationality"), StringLiteral("French")),
//      Statement(javert, NamedElement("nationality"), StringLiteral("French")))
//    s.unsafeRunSync()._2.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)),
//      Statement(javert, NamedElement("prisonNumber"), LongLiteral(24602)))
//    s.unsafeRunSync()._3.map((ps: PersistedStatement) => ps.statement).toSet shouldBe Set(
//      Statement(valjean, NamedElement("prisonNumber"), LongLiteral(24601)))
//
//
//  }
}
