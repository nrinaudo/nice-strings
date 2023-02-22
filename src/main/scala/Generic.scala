import Filter.Result
import Filter.Result.{Continue, Fail, Stop, Succeed}

/* This is arguably a little overkill, but it was fun to write.
 *
 * The idea is that the process of string analysis can be turned into two functions:
 * - single char analysis.
 * - end-of-string analysis (some conditions can only be checked once we know the string is complete, such as the
 *   absence of certain substrings).
 *
 * Given that simple API, expressed with `Filter.addChar` and `Filter.complete`, we want to:
 * - be able to terminate analysis early, as soon as we know a result for sure.
 * - compose various filters.
 *
 * The first requirement is easy enough: `Filter.addChar` returns a type that expresses _I'm done_ or _Keep going, and
 * this is my new state_.
 * That new state is interesting for two reasons:
 * - it allows us to keep the API entirely immutable. Instead of mutating internal state, we create a new filter with
 *   the right state and ask the analyser to keep working with that.
 * - it allows us to simplify compound filters: when one is known to have succeeded, for example, we can just drop it
 *   from the list of filters to apply.
 *
 * The second requirement is implemented by having a special `Filter.Compound` filter, which takes two filters and
 * applies them both. The combination rules are not particularly hard:
 * - if either filter fails, fail
 * - if both filters succeed, succeed
 * - otherwise, keep going with the filter(s) that aren't a success or a failure yet.
 */

// - Basic string filter -----------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
/** Generic string filters, used to accumulate state as a string is explored.
  *
  * It has support for early termination, should a failure condition be identified.
  */
trait Filter:
  // - String analysis -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def addChar(c: Char): Result

  /** Called once the end of the string has been reached. */
  def complete: Stop

  // - Composition -----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  // The following functions enable composition (by aggregation) of multiple filters.
  def and(next: Filter): Filter = Filter.Compound(this, next)
  def &&(next: Filter): Filter  = and(next)

  // - Actual analysis -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def matches(str: String): Boolean =
    def toBoolean(stop: Stop) = stop match
      case Fail    => false
      case Succeed => true

    @annotation.tailrec
    def loop(filter: Filter, curr: List[Char]): Boolean =
      curr match
        case head :: tail =>
          filter.addChar(head) match
            case Continue(next) => loop(next, tail)
            case stop: Stop     => toBoolean(stop)
        case _ => toBoolean(filter.complete)

    loop(this, str.toList)

object Filter:
  // - Analys result type ----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  enum Result:
    /** No need for further analysis: the string will not match. */
    case Fail extends Result with Result.Stop

    /** No need for further analysis: the string will match. */
    case Succeed extends Result with Result.Stop

    /** Not enough info, keep going with the specified filter. */
    case Continue(next: Filter)

  object Result:
    /** Used to tag "early termination" types. */
    sealed trait Stop

  // - At least MIN chars match PREDICATE ------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def minVowels(min: Int): Filter =
    val vowels = Set('a', 'e', 'i', 'o', 'u')
    Min(vowels.contains _, 0, min)

  case class Min(matches: Char => Boolean, current: Int, min: Int) extends Filter:
    override def addChar(c: Char) =
      if matches(c) then
        if current + 1 >= min then Succeed
        else Continue(copy(current = current + 1))
      else Continue(this)

    override def complete = Fail

  // - At least one sequence of two identical characters ---------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  // We're adding a first "layer" of filter whose sole purpose is to read the first character, and delegate the rest
  // of the work to `Repeated`. This allows us to always have a previous char in `Repeated`, which makes things
  // easier.
  def withRepeating(min: Int): Filter = new Filter:
    override def addChar(c: Char) =
      if min == 1 then Succeed
      else Continue(Repeated(c, 1, min))

    override def complete = Fail

  case class Repeated(prev: Char, current: Int, min: Int) extends Filter:
    override def addChar(c: Char) =
      if prev == c then
        if current + 1 >= min then Succeed
        else Continue(copy(prev = c, current = current + 1))
      else Continue(copy(prev = c, current = 1))

    override def complete = Fail

  // - Must not contain STRING -----------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def without(str: String): Filter = ForbiddenSubstring(str, "")

  case class ForbiddenSubstring(str: String, curr: String) extends Filter:
    override def addChar(c: Char) =
      val next = s"$curr$c"

      if next == str then Fail
      else if next.length >= str.length then Continue(copy(curr = s"$c"))
      else Continue(copy(curr = next))

    override def complete = Succeed

  // - Compound filter -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  // Composes two filters.
  case class Compound(left: Filter, right: Filter) extends Filter:
    override def addChar(c: Char) = left.addChar(c) match
      case Continue(nextLeft) =>
        right.addChar(c) match
          case Continue(nextRight) =>
            Continue(Compound(nextLeft, nextRight))

          case Fail    => Fail
          case Succeed => Continue(nextLeft)

      case Fail    => Fail
      case Succeed => right.addChar(c)

    override def complete = left.complete match
      case Succeed => right.complete
      case Fail    => Fail

// - Test code ---------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------

@main def run =
  import Filter.*

  val filter = minVowels(3) && withRepeating(2) && without("ab") && without("cd") && without("pq") && without("xy")

  // Keeps track of the total number of strings, and how many "nice" ones we encountered.
  case class Accumulator(nice: Int = 0, total: Int = 0):
    def addLine(matches: Boolean) =
      if matches then Accumulator(nice + 1, total + 1)
      else copy(total = total + 1)

  val result = scala.io.Source
    .fromResource("sample-input.txt")
    .getLines()
    .foldLeft(Accumulator()) { case (acc, line) =>
      acc.addLine(filter.matches(line))
    }

  println(s"${result.nice} nice strings out of ${result.total}")
