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

  val testCollection: CollectionName = CollectionName("test")

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
        _ <- tx.deleteCollection(CollectionName("Test2"))
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
          s <- tx.allTriples(testCollection)
        } yield s)

        s.unsafeRunSync().toSet shouldBe Set()
      }
    }.unsafeRunSync()
  }

  it should "adding statements to collections" in {
    createSession().use { store =>
      IO {
        store.write.use(tx => for {
          _ <- tx.addTriple(testCollection, Triple(Node(StringLiteral("Alex")), Edge("isa"), Node(StringLiteral("Human"))))
          r <- tx.addTriple(testCollection, Triple(Node(StringLiteral("Clarice")), Edge("isa"), Node(StringLiteral("Feline"))))
        } yield r).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.allTriples(testCollection)
        } yield s)

        s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe
          Set(Triple(Node(StringLiteral("Alex")), Edge("isa"), Node(StringLiteral("Human"))),
            Triple(Node(StringLiteral("Clarice")), Edge("isa"), Node(StringLiteral("Feline"))))

      }
    }.unsafeRunSync()
  }

//  it should "removing statements from collections" in {
//    createSession().use { store => IO {
//
//    store.write.use( tx => for {
//      _ <- tx.addTriple(testCollection, Triple(Node(StringLiteral("Alex"), Edge("isa"), Node(StringLiteral("Human")))
//      _ <- tx.addTriple(testCollection, Triple(Node(StringLiteral("Clarice"), Edge("isa"), Node(StringLiteral("Feline")))
//      x <- tx.removeTriple(testCollection, Triple(Node(StringLiteral("Alex"), Edge("isa"), Node(StringLiteral("Human")))
//    } yield x).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allTriples(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe
//      Set(Triple(Node(StringLiteral("Clarice"), Edge("isa"), Node(StringLiteral("Feline")))
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
          _ <- tx.addTriple(testCollection, Triple(e0.get, Edge("isa"), e1.get))
          _ <- tx.addTriple(testCollection, Triple(e2.get, Edge("isa"), e3.get))
        } yield ()).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.allTriples(testCollection)
        } yield s)

        s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(AnonymousNode(0), Edge("isa"), AnonymousNode(1)),
          Triple(AnonymousNode(2), Edge("isa"), AnonymousNode(3)))
      }
    }.unsafeRunSync()
  }

//  it should "removing named entity" in {
//    createSession().use { store => IO {
//    val entA = Node(StringLiteral("a")
//    val entB = Node(StringLiteral("b")
//    val entC = Node(StringLiteral("c")
//
//    store.write.use( tx => for {
//      _ <- tx.addTriple(testCollection, Triple(entA, Edge("isa"), entB))
//      _ <- tx.addTriple(testCollection, Triple(entC, Node(StringLiteral("a"), entB))
//      _ <- tx.addTriple(testCollection, Triple(entB, Edge("isa"), entA))
//      _ <- tx.removeEntity(testCollection, entA)
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allTriples(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe
//      Set(Triple(Node(StringLiteral("c"), Node(StringLiteral("a"), Node(StringLiteral("b")))
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
//      _ <- tx.addTriple(testCollection, Triple(ent1.get, Edge("isa"), ent2.get))
//      _ <- tx.addTriple(testCollection, Triple(ent3.get, Edge("isa"), ent2.get))
//      _ <- tx.addTriple(testCollection, Triple(ent2.get, Edge("isa"), ent1.get))
//      _ <- tx.removeEntity(testCollection, ent1.get)
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allTriples(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe
//      Set(Triple(AnonymousNode(3), Edge("isa"), AnonymousNode(2)))
//
//  }
//
//  it should "removing NamedElement" in {
//    createSession().use { store => IO {
//    val namedA = Node(StringLiteral(Edge("isa").identifier)
//    store.write.use( tx => for {
//      ent1 <- tx.newEntity(testCollection)
//      _ <- tx.addTriple(testCollection, Triple(ent1.get, Edge("isa"), Node(StringLiteral("test")))
//      _ <- tx.addTriple(testCollection, Triple(namedA, Node(StringLiteral("test"), namedA))
//      _ <- tx.addTriple(testCollection, Triple(namedA, Edge("isa"), ent1.get))
//      _ <- tx.removeNode(StringLiteral(testCollection, Edge("isa"))
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.allTriples(testCollection)
//    } yield s)
//
//    s.unsafeRunSync().map((ts: PersistedTriple) => ts.triple).toSet shouldBe
//      Set(Triple(namedA, Node(StringLiteral("test"), namedA))
//
//  }

  it should "matching against a non-existant collection" in {
    createSession().use { store =>
      IO {
        val s = store.compute.use(tx => for {
          s <- tx.matchTriples(testCollection, None, None, Some(StringLiteral("French")))
          s2 <- tx.matchTriples(testCollection, None, Some(Edge("isa")), None)
        } yield (s, s2))

        s.unsafeRunSync()._1.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set()
        s.unsafeRunSync()._2.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set()

      }
    }.unsafeRunSync()
  }

  it should "matching statements in collections" in {
    createSession().use { store =>
      IO {
        val valjean = Node(StringLiteral("valjean"))
        val javert = Node(StringLiteral("javert"))

        store.write.use(tx => for {
          _ <- tx.addTriple(testCollection, Triple(valjean, Edge("nationality"), Node(StringLiteral("French"))))
          _ <- tx.addTriple(testCollection, Triple(valjean, Edge("prisonNumber"), Node(LongLiteral(24601))))
          _ <- tx.addTriple(testCollection, Triple(javert, Edge("nationality"), Node(StringLiteral("French"))))
        } yield ()).unsafeRunSync()

        val s = store.compute.use(tx => for {
          s <- tx.matchTriples(testCollection, None, None, Some(StringLiteral("French")))
          s2 <- tx.matchTriples(testCollection, None, None, Some(LongLiteral(24601)))
          s3 <- tx.matchTriples(testCollection, Some(valjean))
          s4 <- tx.matchTriples(testCollection, Some(javert),
            Some(Edge("nationality")),
            Some(StringLiteral("French")))
          s5 <- tx.matchTriples(testCollection, None, None, None)
        } yield (s, s2, s3, s4, s5))

        s.unsafeRunSync()._1.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(valjean, Edge("nationality"), StringLiteral("French")),
          Triple(javert, Edge("nationality"), StringLiteral("French")))
        s.unsafeRunSync()._2.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(valjean, Edge("prisonNumber"), LongLiteral(24601)))
        s.unsafeRunSync()._3.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(valjean, Edge("nationality"), StringLiteral("French")),
          Triple(valjean, Edge("prisonNumber"), LongLiteral(24601)))
        s.unsafeRunSync()._4.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(javert, Edge("nationality"), StringLiteral("French")))
        s.unsafeRunSync()._5.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
          Triple(valjean, Edge("nationality"), StringLiteral("French")),
          Triple(valjean, Edge("prisonNumber"), LongLiteral(24601)),
          Triple(javert, Edge("nationality"), StringLiteral("French")))
      }
    }.unsafeRunSync()
  }

//  it should "matching statements with literals and ranges in collections" in {
//    createSession().use { store => IO {
//    val valjean = Node(StringLiteral("valjean")
//    val javert = Node(StringLiteral("javert")
//    val trout = Node(StringLiteral("trout")
//
//    store.write.use( tx => for {
//      _ <- tx.addTriple(testCollection, Triple(valjean, Node(StringLiteral("nationality"), StringLiteral("French")))
//      _ <- tx.addTriple(testCollection, Triple(valjean, Node(StringLiteral("prisonNumber"), LongLiteral(24601)))
//      _ <- tx.addTriple(testCollection, Triple(javert, Node(StringLiteral("nationality"), StringLiteral("French")))
//      _ <- tx.addTriple(testCollection, Triple(javert, Node(StringLiteral("prisonNumber"), LongLiteral(24602)))
//      _ <- tx.addTriple(testCollection, Triple(trout, Node(StringLiteral("nationality"), StringLiteral("American")))
//      _ <- tx.addTriple(testCollection, Triple(trout, Node(StringLiteral("prisonNumber"), LongLiteral(24603)))
//    } yield ()).unsafeRunSync()
//
//    val s = store.compute.use( tx => for {
//      s <- tx.matchTriples(testCollection, None, None, Range(StringLiteral("French"), StringLiteral("German")))
//      s2 <- tx.matchTriples(testCollection, None, None, Range(LongLiteral(24601), LongLiteral(24603)))
//      s3 <- tx.matchTriples(testCollection, Some(valjean), None, Range(LongLiteral(24601), LongLiteral(24603)))
//    } yield (s, s2, s3))
//
//    s.unsafeRunSync()._1.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
//      Triple(valjean, Node(StringLiteral("nationality"), StringLiteral("French")),
//      Triple(javert, Node(StringLiteral("nationality"), StringLiteral("French")))
//    s.unsafeRunSync()._2.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
//      Triple(valjean, Node(StringLiteral("prisonNumber"), LongLiteral(24601)),
//      Triple(javert, Node(StringLiteral("prisonNumber"), LongLiteral(24602)))
//    s.unsafeRunSync()._3.map((ts: PersistedTriple) => ts.triple).toSet shouldBe Set(
//      Triple(valjean, Node(StringLiteral("prisonNumber"), LongLiteral(24601)))
//
//
//  }
}
