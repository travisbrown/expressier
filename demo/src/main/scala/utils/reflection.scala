package expressier.utils

import scala.reflect.api.Universe

trait JavaReflectionUtils {
  protected def getField(clazz: Class[_], name: String) = {
    val field = clazz.getDeclaredField(name)
    field.setAccessible(true)
    field
  }

  protected def getMethod(clazz: Class[_], name: String) = {
    val method = clazz.getDeclaredMethod(name)
    method.setAccessible(true)
    method
  }
}

trait ScalaReflectionUtils {
  def constructor(u: Universe) = {
    import u._
          
    DefDef(
      Modifiers(),
      nme.CONSTRUCTOR,
      Nil,
      Nil :: Nil,
      TypeTree(),
      Block(
        Apply(
          Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR),
          Nil
        ) :: Nil,
        Literal(Constant(()))
      )
    )
  }

  def constructorWithSameParameters(u: Universe)(
    parameters: List[(u.TermName, u.Type)]
  ) = {
    import u._
          
    DefDef(
      Modifiers(),
      nme.CONSTRUCTOR,
      Nil,
      parameters.map {
        case (name, tpe) =>
          ValDef(Modifiers(Flag.PARAM), name, TypeTree(tpe), EmptyTree)
      } :: Nil,
      TypeTree(),
      Block(
        Apply(
          Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR),
          parameters.map { case (name, _) => Ident(name) }
        ) :: Nil,
        Literal(Constant(()))
      )
    )
  }
}

