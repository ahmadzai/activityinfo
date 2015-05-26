@web
Feature: Calculated fields

  Background:
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have created a form named "NFI Distribution"

  @AI-991
  Scenario: Calculating Percentages.
    Given I have created a quantity field "a" in "NFI Distribution" with code "a"
    And I have created a quantity field "b" in "NFI Distribution" with code "b"
    And I have created a calculated field "c" in "NFI Distribution" with expression "{a}/{b}"
    When I submit a "NFI Distribution" form with:
      | field              | value           |
      | a                  | 1               |
      | b                  | 2               |
      | partner            | NRC             |
    Then the submission's detail shows:
      | field              | value           |
      | a                  | 1               |
      | b                  | 2               |
      | c                  | 0.5             |
    When I update the submission with:
      | field              | value           |
      | a                  | 1               |
      | b                  | 0               |
    Then the submission's detail shows:
      | field              | value           |
      | a                  | 1               |
      | c                  | ∞              |
    When I update the submission with:
      | field              | value           |
      | a                  | 0               |
      | b                  | 0               |
    Then the submission's detail shows:
      | field              | value           |
      | c                  | NaN             |

  @AI-1041
  Scenario: Pivot calculated indicator.
    Given I have created a quantity field "total" in "NFI Distribution" with code "total"
    And I have created a quantity field "withHelmet" in "NFI Distribution" with code "helmet"
    And I have created a calculated field "percent" in "NFI Distribution" with expression "({helmet}/{total})*100" with aggregation "Average"
    And I submit a "NFI Distribution" form with:
      | field      | value  |
      | total      | 300    |
      | withHelmet | 150    |
    And I submit a "NFI Distribution" form with:
      | field      | value  |
      | total      | 500    |
      | withHelmet | 50     |
    And I submit a "NFI Distribution" form with:
      | field      | value  |
      | total      | 100    |
      | withHelmet | 90     |
    Then aggregating the indicator "percent" by Indicator should yield:
      |                  | Value |
      | percent          | 50    |

  @AI-1082
  Scenario: Drill down on calculated indicator.
    Given I have created a quantity field "i1" in "NFI Distribution" with code "i1"
    And I have created a quantity field "i2" in "NFI Distribution" with code "i2"
    And I have created a calculated field "plus" in "NFI Distribution" with expression "{i1}+{i2}" with aggregation "Average"
    And I have created a calculated field "percent" in "NFI Distribution" with expression "({i1}/{i2})*100" with aggregation "Sum"
    And I submit a "NFI Distribution" form with:
      | field      | value      |
      | partner    | NRC        |
      | i1         | 300        |
      | i2         | 150        |
      | Start Date | 2014-05-21 |
      | End Date   | 2014-05-21 |
    And I submit a "NFI Distribution" form with:
      | field      | value      |
      | partner    | NRC        |
      | i1         | 100        |
      | i2         | 10         |
      | Start Date | 2014-07-21 |
      | End Date   | 2014-07-21 |
    And I submit a "NFI Distribution" form with:
      | field      | value      |
      | partner    | NRC        |
      | i1         | 4          |
      | i2         | 20         |
      | Start Date | 2015-05-21 |
      | End Date   | 2015-05-21 |
    And I submit a "NFI Distribution" form with:
      | field      | value      |
      | partner    | NRC        |
      | i1         | 5          |
      | i2         | 50         |
      | Start Date | 2015-07-21 |
      | End Date   | 2015-07-21 |
    Then aggregating the indicators plus and percent by Indicator and Year should yield:
      |                  | 2014  | 2015 |
      | plus             | 280   | 39.5 |
      | percent          | 1,200 | 30   |
    Then drill down on "280" should yield:
      | NRC      | RDC  | 2014-07-21 | | 110   |
      | NRC      | RDC  | 2014-05-21 | | 450   |
