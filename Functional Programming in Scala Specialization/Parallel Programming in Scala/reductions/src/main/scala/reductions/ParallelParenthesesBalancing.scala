package reductions

import scala.annotation._
import org.scalameter._
import common._

object ParallelParenthesesBalancingRunner {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime ms")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime ms")
    println(s"speedup: ${seqtime / fjtime}")
  }
}

object ParallelParenthesesBalancing {

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def balance(chars: Array[Char]): Boolean = {
    def counter(index: Int , coun: Int): Int = {
      if (index == chars.length || coun < 0) coun
      else if (chars(index) == '(') counter(index + 1 , coun + 1)
      else if (chars(index) == ')') counter(index + 1 , coun - 1)
      else counter( index + 1 , coun)
    }
    counter(0 ,  0) == 0
  }

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def parBalance(chars: Array[Char], threshold: Int): Boolean = {

    def traverse(idx: Int, until: Int, arg1: Int, arg2: Int) : (Int , Int) = {
      var min = 0
      var coun = 0
      for( i <- idx until until) {
        if( chars(i) == '(' ) coun += 1
        if( chars(i) == ')' ) coun -= 1
        if(coun < min) min = coun
      }
      (min , coun)
    }

    def reduce(from: Int, until: Int) : (Int, Int) = {
      if ( until - from <= threshold ) traverse(from , until , 0 , 0)
      else
        {
          val mid = from + (until - from) / 2
          val (left , right) = parallel(reduce(from , mid) , reduce(mid, until))
          (Math.min(left._1 , left._2 + right._1 ) , left._2 + right._2)
        }
    }

    reduce(0, chars.length) == (0 , 0)
  }

  // For those who want more:
  // Prove that your reduction operator is associative!

}
