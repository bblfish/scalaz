package scalaz

import org.scalacheck.Prop
import org.scalacheck.Prop.forAll

object ISetTest extends SpecLite {
  import org.scalacheck.Arbitrary
  import scalaz.scalacheck.ScalazProperties._
  import scalaz.scalacheck.ScalazArbitrary._
  import syntax.std.option._
  import std.anyVal._
  import std.list._
  import std.option._
  import std.tuple._

  import ISet._

  // Check some laws
  checkAll(equal.laws[ISet[Int]])
  checkAll(order.laws[ISet[Int]])
  checkAll(monoid.laws[ISet[Int]])
  checkAll(foldable.laws[ISet])

  def structurallySound[A: Order: Show](s: ISet[A]) = {
    val al = s.toAscList
    al must_===(al.sorted)(Order[A].toScalaOrdering)
  }

  "split" ! forAll { (a: ISet[Int], i: Int) =>
    val (b, c) = a.split(i)
    structurallySound(b)
    structurallySound(c)
    Foldable[ISet].all(b)(_ < i) must_=== true
    Foldable[ISet].all(c)(_ > i) must_=== true
    if(a member i){
      (b.size + c.size + 1) must_=== a.size
      b.union(c).insert(i) must_=== a
    }else{
      (b union c) must_=== a
    }
  }

  "splitMember" ! forAll { (a: ISet[Int], i: Int) =>
    val (b, c, d) = a.splitMember(i)
    structurallySound(b)
    structurallySound(d)
    c must_=== a.member(i)
    Foldable[ISet].all(b)(_ < i) must_=== true
    Foldable[ISet].all(d)(_ > i) must_=== true
    if(c){
      (b.size + d.size + 1) must_=== a.size
      b.union(d).insert(i) must_=== a
    }else{
      (b union d) must_=== a
    }
  }

  "lookupLT" ! forAll { (a: ISet[Int], i: Int) =>
    a.lookupLT(i) match {
      case Some(b) =>
        (i > b) must_=== true
        val (c, d) = a.split(i)
        c.findMax must_=== Option(b)
        Foldable[ISet].all(d)(_ > i) must_=== true
        a.filter(x => b < x && x < i) must_=== ISet.empty
      case None =>
        a.split(i)._1 must_=== ISet.empty
    }
  }

  "member" ! forAll {(a: ISet[Int], i: Int) =>
    a.member(i) must_=== a.toList.contains(i)
  }

  "sound delete" ! forAll {(a: ISet[Int], i: Int) =>
    val b = a.delete(i)
    structurallySound(b)
    if(a.member(i))
      (a.size - b.size) must_=== 1
    else
      a must_=== b
  }

  "sound insert" ! forAll {(a: ISet[Int], i: Int) =>
    val b = a.insert(i)
    structurallySound(b)
    if(a.member(i))
      a must_=== b
    else
      (b.size - a.size) must_=== 1
  }

  "sound union" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    (a union b) must_=== ISet.fromList(a.toList ++ b.toList)
    structurallySound(a union b)
  }

  "union commute" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    (a union b) must_===(b union a)
  }

  "union idempotent" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    val ab = a union b
    ab union a must_===(ab)
    ab union b must_===(ab)
  }

  "sound intersection" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    structurallySound(a intersection b)
  }

  "intersection commute" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    (a intersection b) must_===(b intersection a)
  }

  """\\ is idempotent""" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    val ab = a \\ b
    (ab \\ b).toList must_===(ab.toList)
  }

  "difference is an inverse" ! forAll {(a: ISet[Int]) =>
    (a difference a) must_===(ISet.empty[Int])
  }

  "sound difference" ! forAll {(a: ISet[Int], b: ISet[Int]) =>
    val c = a difference b
    structurallySound(c)
    Foldable[ISet].any(c)(b member _) must_=== false
    Foldable[ISet].all(c)(a member _) must_=== true
  }

  "filter" ! forAll {(a: ISet[Int], p: Int => Boolean) =>
    (a filter p).toList must_=== a.toList.filter(p)
  }

  "sound partition" ! forAll {(a: ISet[Int], p: Int => Boolean) =>
    val (ma, mb) = a partition p
    structurallySound(ma)
    structurallySound(mb)
    (ma.size + mb.size) must_=== a.size
    (ma union mb) must_=== a
  }

  "partition" ! forAll {(a: ISet[Int], p: Int => Boolean) =>
    val part = a partition p
    (part._1.toList, part._2.toList) must_=== a.toList.partition(p)
  }

  "map" ! forAll {(a: ISet[Int], f: Int => Int) =>
    a.map(f).toList must_=== a.toList.map(f).distinct.sorted
  }

  "filter" ! forAll {(s: ISet[Int], p: Int => Boolean) =>
    s.filter(p).toList must_=== s.toList.sorted.filter(p)
  }

  "findMin" ! forAll {(a: ISet[Int]) =>
    a.findMin must_=== a.toList.sorted.headOption
  }

  "findMax" ! forAll {(a: ISet[Int]) =>
    a.findMax must_=== a.toList.sortWith(_ > _).headOption
  }

  "deleteMin" ! forAll {(a: ISet[Int]) =>
    a.deleteMin must_=== fromList(a.toList.drop(1))
  }

  "deleteMax" ! forAll {(a: ISet[Int]) =>
    a.deleteMax must_=== fromList(a.toList.sortWith(_ > _).drop(1))
  }

  "minView" ! forAll {(a: ISet[Int]) =>
    val l = a.toList.sorted
    val target = if (l.isEmpty) none else (l.head, fromList(l.tail)).some
    a.minView must_=== target
  }

  "maxView" ! forAll {(a: ISet[Int]) =>
    val l = a.toList.sortWith(_ > _)
    val target = if (l.isEmpty) none else (l.head, fromList(l.tail)).some
    a.maxView must_=== target
  }

  "isSubsetOf" ! forAll { (a: ISet[Int], b: ISet[Int]) =>
    val c = a isSubsetOf b
    c must_=== (a.toList.toSet subsetOf b.toList.toSet)
    c must_=== Foldable[ISet].all(a)(b member _)
    (c && (b isSubsetOf a)) must_=== Equal[ISet[Int]].equal(a, b)
  }

}
