package com.github.tarao
package slickjdbc
package interpolation

import helper.UnitSpec

class PlaceholderSpec extends UnitSpec {
  import PlaceholderSpec._

  def toPlaceholder[T](value: T)(implicit p: ToPlaceholder[T]): Placeholder =
    p(value)

  describe("Placeholder of simple types") {
    it("should be a Literal") {
      val p1 = toPlaceholder(1)
      p1 shouldBe a [Literal]

      val p2 = toPlaceholder("foo")
      p2 shouldBe a [Literal]

      val p3 = toPlaceholder((1, "foo"))
      p3 shouldBe a [Literal]
    }

    describe(".toTopLevelString") {
      it("should print an empty string for a simple type") {
        val p1 = toPlaceholder(1)
        p1.toTopLevelString should equal ("")
        val p2 = toPlaceholder("foo")
        p2.toTopLevelString should equal ("")
        val p3 = toPlaceholder(Foo(1, "foo"))
        p3.toTopLevelString should equal ("")
        val p4 = toPlaceholder(new Bar(1, "foo"))
        p4.toTopLevelString should equal ("")
      }

      it("should print '?'s x (tuple size - 1)") {
        locally {
          val p1 = toPlaceholder((1, "foo", 3))
          p1.toTopLevelString should equal ("?, ?, ")
          val p2 = toPlaceholder((1, "foo", 3, "bar", 5))
          p2.toTopLevelString should equal ("?, ?, ?, ?, ")
          val p3 = toPlaceholder(Tuple1(1))
          p3.toTopLevelString should equal ("")
        }

        locally {
          val p1 = toPlaceholder(Tuple1(1))
          p1.toTopLevelString should equal ("")
          val p2 = toPlaceholder((1, 2))
          p2.toTopLevelString should equal ("?, ")
          val p3 = toPlaceholder((1, 2, 3))
          p3.toTopLevelString should equal ("?, ?, ")
          val p4 = toPlaceholder((1, 2, 3, 4))
          p4.toTopLevelString should equal ("?, ?, ?, ")
          val p5 = toPlaceholder((1, 2, 3, 4, 5))
          p5.toTopLevelString should equal ("?, ?, ?, ?, ")
          val p6 = toPlaceholder((1, 2, 3, 4, 5, 6))
          p6.toTopLevelString should equal ("?, ?, ?, ?, ?, ")
          val p7 = toPlaceholder((1, 2, 3, 4, 5, 6, 7))
          p7.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ")
          val p8 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8))
          p8.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ")
          val p9 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9))
          p9.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ")
          val p10 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
          p10.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p11 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
          p11.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p12 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
          p12.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p13 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
          p13.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p14 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
          p14.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p15 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
          p15.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p16 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
          p16.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p17 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
          p17.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p18 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
          p18.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p19 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))
          p19.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p20 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
          p20.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p21 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
          p21.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
          val p22 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22))
          p22.toTopLevelString should equal ("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
        }
      }

      it("should print nested '?'s for nested tuples") {
        val p1 = toPlaceholder((("foo", 1), ("bar", 2)))
        p1.toTopLevelString should equal ("?, ?), (?, ")
      }

      it("should not look into nested products") {
        val p1 = toPlaceholder(Baz("simple", 1))
        p1.toTopLevelString should equal ("")
        val p2 = toPlaceholder(Some(1))
        p2.toTopLevelString should equal ("")
        val p3 = toPlaceholder(Some(Baz("single", 1)))
        p3.toTopLevelString should equal ("")
        val p4 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p4.toTopLevelString should equal ("")
      }

      it("should be customizable") {
        class FromPair[T1, T2](p1: ToPlaceholder[T1], p2: ToPlaceholder[T2])
            extends ToPlaceholder[Pair[T1, T2]] {
          def apply(value: Pair[T1, T2]): Placeholder = Placeholder.Nested(
            p1(value.a),
            p2(value.b)
          )
        }
        implicit def pairToPlaceholder[T1, T2](implicit
          p1: ToPlaceholder[T1],
          p2: ToPlaceholder[T2]
        ): ToPlaceholder[Pair[T1, T2]] = new FromPair[T1, T2](p1, p2)

        val p1 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p1.toTopLevelString should equal ("?, ")
        val p2 = toPlaceholder(Pair((1, 2), (3, 4)))
        p2.toTopLevelString should equal ("?, ?), (?, ")
      }
    }

    describe(".toString") {
      it("should print a single '?' for a simple type") {
        val p1 = toPlaceholder(1)
        p1.toString should equal ("?")
        val p2 = toPlaceholder("foo")
        p2.toString should equal ("?")
        val p3 = toPlaceholder(Foo(1, "foo"))
        p3.toString should equal ("?")
        val p4 = toPlaceholder(new Bar(1, "foo"))
        p4.toString should equal ("?")
      }

      it("should print '?'s x (tuple size) with parenthesis") {
        locally {
          val p1 = toPlaceholder((1, "foo", 3))
          p1.toString should equal ("(?, ?, ?)")
          val p2 = toPlaceholder((1, "foo", 3, "bar", 5))
          p2.toString should equal ("(?, ?, ?, ?, ?)")
          val p3 = toPlaceholder(Tuple1(1))
          p3.toString should equal ("(?)")
        }

        locally {
          val p1 = toPlaceholder(Tuple1(1))
          p1.toString should equal ("(?)")
          val p2 = toPlaceholder((1, 2))
          p2.toString should equal ("(?, ?)")
          val p3 = toPlaceholder((1, 2, 3))
          p3.toString should equal ("(?, ?, ?)")
          val p4 = toPlaceholder((1, 2, 3, 4))
          p4.toString should equal ("(?, ?, ?, ?)")
          val p5 = toPlaceholder((1, 2, 3, 4, 5))
          p5.toString should equal ("(?, ?, ?, ?, ?)")
          val p6 = toPlaceholder((1, 2, 3, 4, 5, 6))
          p6.toString should equal ("(?, ?, ?, ?, ?, ?)")
          val p7 = toPlaceholder((1, 2, 3, 4, 5, 6, 7))
          p7.toString should equal ("(?, ?, ?, ?, ?, ?, ?)")
          val p8 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8))
          p8.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?)")
          val p9 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9))
          p9.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p10 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
          p10.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p11 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
          p11.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p12 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
          p12.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p13 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
          p13.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p14 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
          p14.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p15 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
          p15.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p16 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
          p16.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p17 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
          p17.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p18 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
          p18.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p19 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))
          p19.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p20 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
          p20.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p21 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
          p21.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
          val p22 = toPlaceholder((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22))
          p22.toString should equal ("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        }
      }

      it("should print nested '?'s for nested tuples") {
        val p1 = toPlaceholder((("foo", 1), ("bar", 2)))
        p1.toString should equal ("(?, ?), (?, ?)")
      }

      it("should not look into nested products") {
        val p1 = toPlaceholder(Baz("simple", 1))
        p1.toString should equal ("?")
        val p2 = toPlaceholder(Some(1))
        p2.toString should equal ("?")
        val p3 = toPlaceholder(Some(Baz("single", 1)))
        p3.toString should equal ("?")
        val p4 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p4.toString should equal ("?")
      }

      it("should be customizable") {
        class FromPair[T1, T2](p1: ToPlaceholder[T1], p2: ToPlaceholder[T2])
            extends ToPlaceholder[Pair[T1, T2]] {
          def apply(value: Pair[T1, T2]): Placeholder = Placeholder.Nested(
            p1(value.a),
            p2(value.b)
          )
        }
        implicit def pairToPlaceholder[T1, T2](implicit
          p1: ToPlaceholder[T1],
          p2: ToPlaceholder[T2]
        ): ToPlaceholder[Pair[T1, T2]] = new FromPair[T1, T2](p1, p2)

        val p1 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p1.toString should equal ("(?, ?)")
        val p2 = toPlaceholder(Pair((1, 2), (3, 4)))
        p2.toString should equal ("(?, ?), (?, ?)")
      }
    }
  }

  describe("Placeholder of compound parameters") {
    import eu.timepit.refined.collection.NonEmpty
    import eu.timepit.refined.refineV
    import CompoundParameter._

    it("should be a Literal") {
      val p1 = toPlaceholder(Foo(1, "foo"))
      p1 shouldBe a [Literal]

      val Right(nel) = refineV[NonEmpty](Seq(1, 2, 3))
      val p2 = toPlaceholder(nel)
      p2 shouldBe a [Literal]

      val p3 = toPlaceholder(util.NonEmpty(1, 2, 3))
      p3 shouldBe a [Literal]

    }

    it("should not be instantiated by a nullary product") {
      a[java.sql.SQLException] should be thrownBy toPlaceholder(None)
    }

    describe(".toTopLevelString") {
      it("should print '?'s x (list size - 1)") {
        val Right(nel1) = refineV[NonEmpty](Seq(1, 2, 3))
        val Right(nel2) = refineV[NonEmpty](Seq(1, 2, 3, 4, 5))
        val Right(nel3) = refineV[NonEmpty](Seq(1))

        val p1 = toPlaceholder(nel1)
        p1.toTopLevelString should equal ("?, ?, ")
        val p2 = toPlaceholder(nel2)
        p2.toTopLevelString should equal ("?, ?, ?, ?, ")
        val p3 = toPlaceholder(nel3)
        p3.toTopLevelString should equal ("")

        val p4 = toPlaceholder(util.NonEmpty(1, 2, 3))
        p4.toTopLevelString should equal ("?, ?, ")
        val p5 = toPlaceholder(util.NonEmpty(1, 2, 3, 4, 5))
        p5.toTopLevelString should equal ("?, ?, ?, ?, ")
        val p6 = toPlaceholder(util.NonEmpty(1))
        p6.toTopLevelString should equal ("")
      }

      it("should print '?'s x (product arity - 1)") {
        val p1 = toPlaceholder(Foo(1, "foo"))
        p1.toTopLevelString should equal ("?, ")
      }

      it("should print nested '?'s for a nested structure") {
        val p1 = toPlaceholder(((1, "foo"), (2, "bar"), (3, "baz")))
        p1.toTopLevelString should equal ("?, ?), (?, ?), (?, ")

        val Right(nel1) = refineV[NonEmpty](Seq(1, 2))
        val Right(nel2) = refineV[NonEmpty](Seq(3))
        val Right(nel3) = refineV[NonEmpty](Seq(4, 5))
        val Right(nel4) = refineV[NonEmpty](Seq(nel1, nel2, nel3))
        val Right(nel5) = refineV[NonEmpty](Seq(
          (1, "foo"),
          (2, "bar"),
          (3, "baz")
        ))

        val p2 = toPlaceholder(nel4)
        p2.toTopLevelString should equal ("?, ?), (?), (?, ")
        val p3 = toPlaceholder(nel5)
        p3.toTopLevelString should equal ("?, ?), (?, ?), (?, ")

        val p4 = toPlaceholder(util.NonEmpty(util.NonEmpty(1, 2), util.NonEmpty(3), util.NonEmpty(4, 5)))
        p4.toTopLevelString should equal ("?, ?), (?), (?, ")
        val p5 = toPlaceholder(util.NonEmpty((1, "foo"), (2, "bar"), (3, "baz")))
        p5.toTopLevelString should equal ("?, ?), (?, ?), (?, ")
      }

      it("should look into the structure of a nested product") {
        val p1 = toPlaceholder(Baz("simple", 1))
        p1.toTopLevelString should equal ("?, ")
        val p2 = toPlaceholder(Some(1))
        p2.toTopLevelString should equal ("")
        val p3 = toPlaceholder(Some(Baz("single", 1)))
        p3.toTopLevelString should equal ("?, ")
        val p4 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p4.toTopLevelString should equal ("?, ?, ?, ")
        val p5 = toPlaceholder(Baz("nested", (1, Pair(2, 3))))
        p5.toTopLevelString should equal ("?, ?, ?, ")

        val Right(nel) = refineV[NonEmpty](Seq(1, 2))

        val p6 = toPlaceholder(Baz("a list in product is not expanded", nel))
        p6.toTopLevelString should equal ("?, ")

        val p7 = toPlaceholder(Baz("a list in product is not expanded", util.NonEmpty(1, 2)))
        p7.toTopLevelString should equal ("?, ")
      }

      it("should preserve simple type conversions") {
        locally {
          val p1 = toPlaceholder(1)
          p1.toTopLevelString should equal ("")
          val p2 = toPlaceholder("foo")
          p2.toTopLevelString should equal ("")
          val p3 = toPlaceholder(new Bar(1, "foo"))
          p3.toTopLevelString should equal ("")
        }

        locally {
          val p1 = toPlaceholder((1, "foo", 3))
          p1.toTopLevelString should equal ("?, ?, ")
          val p2 = toPlaceholder((1, "foo", 3, "bar", 5))
          p2.toTopLevelString should equal ("?, ?, ?, ?, ")
          val p3 = toPlaceholder(Tuple1(1))
          p3.toTopLevelString should equal ("")
        }

        locally {
          val p1 = toPlaceholder((("foo", 1), ("bar", 2)))
          p1.toTopLevelString should equal ("?, ?), (?, ")
        }
      }

      it("should be customizable") {
        class FromPair[T1, T2] extends ToPlaceholder[Pair[T1, T2]] {
          def apply(value: Pair[T1, T2]): Placeholder = Placeholder()
        }
        implicit def pairToPlaceholder[T1, T2]: ToPlaceholder[Pair[T1, T2]] =
          new FromPair[T1, T2]

        val p1 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p1.toTopLevelString should equal ("")
        val p2 = toPlaceholder(Pair((1, 2), (3, 4)))
        p2.toTopLevelString should equal ("")
      }
    }

    describe(".toString") {
      it("should print '?'s x (list size) with parenthesis") {
        val Right(nel1) = refineV[NonEmpty](Seq(1, 2, 3))
        val Right(nel2) = refineV[NonEmpty](Seq(1, 2, 3, 4, 5))
        val Right(nel3) = refineV[NonEmpty](Seq(1))

        val p1 = toPlaceholder(nel1)
        p1.toString should equal ("(?, ?, ?)")
        val p2 = toPlaceholder(nel2)
        p2.toString should equal ("(?, ?, ?, ?, ?)")
        val p3 = toPlaceholder(nel3)
        p3.toString should equal ("(?)")

        val p4 = toPlaceholder(util.NonEmpty(1, 2, 3))
        p4.toString should equal ("(?, ?, ?)")
        val p5 = toPlaceholder(util.NonEmpty(1, 2, 3, 4, 5))
        p5.toString should equal ("(?, ?, ?, ?, ?)")
        val p6 = toPlaceholder(util.NonEmpty(1))
        p6.toString should equal ("(?)")
      }

      it("should print '?'s x (product arity - 1)") {
        val p1 = toPlaceholder(Foo(1, "foo"))
        p1.toString should equal ("(?, ?)")
      }

      it("should print nested '?'s for a nested structure") {
        val Right(nel1) = refineV[NonEmpty](Seq(1, 2))
        val Right(nel2) = refineV[NonEmpty](Seq(3))
        val Right(nel3) = refineV[NonEmpty](Seq(4, 5))
        val Right(nel4) = refineV[NonEmpty](Seq(nel1, nel2, nel3))
        val Right(nel5) = refineV[NonEmpty](Seq(
          (1, "foo"),
          (2, "bar"),
          (3, "baz")
        ))

        val p1 = toPlaceholder(nel4)
        p1.toString should equal ("(?, ?), (?), (?, ?)")
        val p2 = toPlaceholder(nel5)
        p2.toString should equal ("(?, ?), (?, ?), (?, ?)")
        val p3 = toPlaceholder(((1, "foo"), (2, "bar"), (3, "baz")))
        p3.toString should equal ("(?, ?), (?, ?), (?, ?)")

        val p4 = toPlaceholder(util.NonEmpty(util.NonEmpty(1, 2), util.NonEmpty(3), util.NonEmpty(4, 5)))
        p4.toString should equal ("(?, ?), (?), (?, ?)")
        val p5 = toPlaceholder(util.NonEmpty((1, "foo"), (2, "bar"), (3, "baz")))
        p5.toString should equal ("(?, ?), (?, ?), (?, ?)")
        val p6 = toPlaceholder(((1, "foo"), (2, "bar"), (3, "baz")))
        p6.toString should equal ("(?, ?), (?, ?), (?, ?)")
      }

      it("should look into the structure of a nested product") {
        val p1 = toPlaceholder(Baz("simple", 1))
        p1.toString should equal ("(?, ?)")
        val p2 = toPlaceholder(Some(1))
        p2.toString should equal ("?")
        val p3 = toPlaceholder(Some(Baz("single", 1)))
        p3.toString should equal ("(?, ?)")
        val p4 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p4.toString should equal ("(?, ?, ?, ?)")
        val p5 = toPlaceholder(Baz("nested", (1, Pair(2, 3))))
        p5.toString should equal ("(?, ?, ?, ?)")

        val Right(nel) = refineV[NonEmpty](Seq(1, 2))
        val p7 = toPlaceholder(Baz("a list in product is not expanded", nel))
        p7.toString should equal ("(?, ?)")
      }

      it("should preserve simple type conversions") {
        locally {
          val p1 = toPlaceholder(1)
          p1.toString should equal ("?")
          val p2 = toPlaceholder("foo")
          p2.toString should equal ("?")
          val p3 = toPlaceholder(new Bar(1, "foo"))
          p3.toString should equal ("?")
        }

        locally {
          val p1 = toPlaceholder((1, "foo", 3))
          p1.toString should equal ("(?, ?, ?)")
          val p2 = toPlaceholder((1, "foo", 3, "bar", 5))
          p2.toString should equal ("(?, ?, ?, ?, ?)")
          val p3 = toPlaceholder(Tuple1(1))
          p3.toString should equal ("(?)")
        }

        locally {
          val p1 = toPlaceholder((("foo", 1), ("bar", 2)))
          p1.toString should equal ("(?, ?), (?, ?)")
        }
      }

      it("should be customizable") {
        class FromPair[T1, T2] extends ToPlaceholder[Pair[T1, T2]] {
          def apply(value: Pair[T1, T2]): Placeholder = Placeholder()
        }
        implicit def pairToPlaceholder[T1, T2]: ToPlaceholder[Pair[T1, T2]] =
          new FromPair[T1, T2]

        val p1 = toPlaceholder(Pair(Baz("nested", 1), Baz("nested", 2)))
        p1.toString should equal ("?")
        val p2 = toPlaceholder(Pair((1, 2), (3, 4)))
        p2.toString should equal ("?")
      }
    }
  }
}
object PlaceholderSpec {
  case class Foo(a: Int, b: String)
  class Bar(a: Int, b: String)

  case class Baz[T](name: String, value: T)
  case class Pair[S, T](a: S, b: T)
}
