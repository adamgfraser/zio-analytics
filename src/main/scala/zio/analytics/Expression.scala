package zio.analytics

sealed abstract class Expression[-A, +B]
object Expression {
  case class Id[A]()                                                     extends Expression[A, A]
  case class Compose[A, B, C](f: Expression[B, C], g: Expression[A, B])  extends Expression[A, C]
  case class FanOut[A, B, C](f: Expression[A, B], g: Expression[A, C])   extends Expression[A, (B, C)]
  case class Split[A, B, C, D](f: Expression[A, C], g: Expression[B, D]) extends Expression[(A, B), (C, D)]
  case class LongLiteral[A](l: Long)                                     extends Expression[A, Long]
  case class StringLiteral[A](s: String)                                 extends Expression[A, String]
  case class BooleanLiteral[A](b: Boolean)                               extends Expression[A, Boolean]

  case object Mul   extends Expression[(Long, Long), Long]
  case object Sum   extends Expression[(Long, Long), Long]
  case object Split extends Expression[(String, String), List[String]]

  case class NthColumn[A <: Product, B](n: Int) extends Expression[A, B]

  implicit class FunctionOps[A, B](f: A =>: B) {
    def >>>[C](g: B =>: C): A =>: C              = Compose(g, f)
    def <<<[C](g: C =>: A): C =>: B              = Compose(f, g)
    def &&&[C](g: A =>: C): A =>: (B, C)         = FanOut(f, g)
    def ***[C, D](g: C =>: D): (A, C) =>: (B, D) = Split(f, g)
  }

  implicit class NumberOps[A](x: A =>: Long) {
    def |+|(y: A =>: Long): A =>: Long = (x &&& y) >>> Sum
    def *(y: A =>: Long): A =>: Long   = (x &&& y) >>> Mul
  }

  implicit class StringOps[A](s: A =>: String) {
    def split(delimiter: A =>: String): A =>: List[String] = (s &&& delimiter) >>> Split
  }

  implicit class TupleOps[A, B, C](tp: A =>: (B, C)) {
    def _1: A =>: B = tp >>> NthColumn(0)
    def _2: A =>: C = tp >>> NthColumn(1)
  }

  implicit def liftLong[A](l: Long): A =>: Long                         = LongLiteral(l)
  implicit def liftString[A](s: String): A =>: String                   = StringLiteral(s)
  implicit def liftTuple[A, B, C](tp: (A =>: B, A =>: C)): A =>: (B, C) = FanOut(tp._1, tp._2)
}
