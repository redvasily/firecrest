package firecrest

import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerModule
import com.fasterxml.jackson.module.scala.modifiers.EitherModule


class DropwizardScalaModule
  extends JacksonModule
    with IteratorModule
    with EnumerationModule
    with OptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with UntypedObjectDeserializerModule
    with EitherModule
{
  override def getModuleName = "DropwizardScalaModule"
}

object DropwizardScalaModule extends DropwizardScalaModule