package com.github.tarao
package slickjdbc
package helper

trait TraitSingletonBehavior { self: UnitSpec =>
  import scala.reflect.Manifest
  import java.lang.Class

  def signatures[T](clazz: Class[T]): Set[String] =
    clazz.getDeclaredMethods.map { x =>
      x.getReturnType.toString + " " + x.getName +
        "(" + x.getParameterTypes.mkString(", ") + ")"
    }.toSet

  /**
    Check a singleton object to export methods in a trait.  The object
    should implement the trait and have exactly the same methods as
    the trait.  This ensures that importing the methods by `with
    TheTrait` and by `import TheSingleton._` have the the same effect.
    */
  def exportingTheTraitMethods[T : Manifest](singleton: Any) = {
    singleton shouldBe a [T]
    val parent = implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]]
    signatures(singleton.getClass) subsetOf (signatures(parent)) shouldBe true
  }
}
