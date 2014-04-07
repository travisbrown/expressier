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
