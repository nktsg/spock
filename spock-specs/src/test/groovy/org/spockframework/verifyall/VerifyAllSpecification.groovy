package org.spockframework.verifyall

import org.spockframework.EmbeddedSpecification
import org.spockframework.runtime.*
import spock.lang.FailsWith

import groovy.transform.*

class VerifyAllSpecification extends EmbeddedSpecification {

  def setup() {
    runner.throwFailure = false
  }

  def "verifyAll"() {
    when:
    def result = runner.runSpecBody("""
        def "test1"() {
          expect:
          verifyAll{
            1 == 2
            3 == 4
          }
        }""")
    then:
    result.failures.size() == 2
    with(result.failures[0].exception, SpockComparisonFailure) {
      expected.trim() == "2"
      actual.trim() == "1"
    }
    with(result.failures[1].exception, SpockComparisonFailure) {
      expected.trim() == "4"
      actual.trim() == "3"
    }
  }

  def "assertion blocks should work as expected (reported only once)"() {
    when:
    def result = runner.runWithImports("""
      import spock.util.concurrent.PollingConditions
      class Test extends Specification {
          def "test1"() {
              when:
                  def x = 2
                  def y = 3
              then:
                  verifyAll {
                      PollingConditions pollingConditions = new PollingConditions()
                      pollingConditions.eventually {
                        x == 3
                        y == 4
                      }
                  }
          }
      }""")
    then:
    result.failures.size() == 1
    result.failures[0].exception instanceof SpockTimeoutError
  }

  def "if exception is not in condition, all already failed conditions should be reported"(){
    when:
    def result = runner.runWithImports("""
      import spock.util.concurrent.PollingConditions
      class Test extends Specification {
          def "test1"() {
              when:
                  def x = 2
                  def y = 3
              then:
                  verifyAll {
                      x == 3
                      y == 4
                      def urlWithSpaces = new URL("abc   ") //Invalid Url Exception
                      urlWithSpaces != null
                  }
          }
      }""")
    then:
    result.failures.size() == 2

    with(result.failures[0].exception, SpockComparisonFailure) {
      expected.trim() == "3"
      actual.trim() == "2"
    }
    with(result.failures[1].exception, SpockComparisonFailure) {
      expected.trim() == "4"
      actual.trim() == "3"
    }
  }

  def "verifyAll with target and failures"() {
    when:
    def result = runner.runWithImports("""
      import org.spockframework.verifyall.Person
      class Test extends Specification {
        def "test1"() {
            given:
            Person p = new Person()
            
            expect:
            verifyAll(p){
              name == 'Bob'
              age == 137
            }
          }
      }""")
    then:
    result.failures.size() == 2
    with(result.failures[0].exception, SpockComparisonFailure) {
      expected.trim() == "Bob"
      actual.trim() == "Fred"
    }
    with(result.failures[1].exception, SpockComparisonFailure) {
      expected.trim() == "137"
      actual.trim() == "42"
    }
  }

  @FailsWith(value = SpockAssertionError, reason = "Target of 'verifyAll' block must not be null")
  def "verifyAll with target null fails"() {
    given:
    Person p = null

    expect:
    verifyAll(p) {
      name == 'Bob'
      age == 137
    }
  }

  def "verifyAll with target and class and failures"() {
    when:
    def result = runner.runWithImports("""
      import org.spockframework.verifyall.Person
      class Test extends Specification {
        def "test1"() {
            given:
            Object p = new Person()
            
            expect:
            verifyAll(p, Person){
              name == 'Bob'
              age == 137
            }
          }
      }""")
    then:
    result.failures.size() == 2
    with(result.failures[0].exception, SpockComparisonFailure) {
      expected.trim() == "Bob"
      actual.trim() == "Fred"
    }
    with(result.failures[1].exception, SpockComparisonFailure) {
      expected.trim() == "137"
      actual.trim() == "42"
    }
  }

  @FailsWith(value = SpockAssertionError, reason = "Target of 'verifyAll' block must not be null")
  def "verifyAll with target null and incompatible class fails"() {
    expect:
    verifyAll(null, Person) {
      name == 'Bob'
      age == 137
    }
  }

  @FailsWith(value = SpockAssertionError, reason = "Expected target of 'verifyAll' block to have type '%s', but got '%s'")
  def "verifyAll with target and incompatible class fails"() {
    given:
    Object p = "bob"

    expect:
    verifyAll(p, Person) {
      name == 'Bob'
      age == 137
    }
  }

  @TypeChecked
  def "verifyAll with target"() {
    given:
    Person p = new Person()

    expect:
    verifyAll(p) {
      name == 'Fred'
      age == 42
    }
  }

  @TypeChecked
  def "verifyAll with target and class"() {
    given:
    Object p = new Person()

    expect:
    verifyAll(p, Person) {
      name == 'Fred'
      age == 42
    }
  }
}


@CompileStatic
class Person {
  String name = "Fred"
  int age = 42
}
