@api
Feature: Site API

  Background:
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have created a published form named "Distributions"
    And I have created a quantity field "a" in "Distributions" with code "a"
    And I have created a quantity field "b" in "Distributions" with code "b"
    And I have created a enumerated field "donor" with items:
      | USAID  |
      | ECHO   |
      | NRC    |
    When I have submitted a "Distributions" form with:
      | field     | value      |
      | a         | 1          |
      | b         | 2          |
      | partner   | NRC        |
      | donor     | USAID      |
      | fromDate  | 2014-01-01 |
      | toDate    | 2014-04-10 |

  @AI-574
  Scenario: Querying site's points on public database with unauthenticated user
    When Unauthenticated user requests /resources/sites/points?activity=$Distributions
    Then the response should be:
    """
    type : FeatureCollection
    features :
     -
        type : Feature
        id : $SiteId
        properties :
          locationName : Rdc
          partnerName : $NRC
          activity : $Distributions
          activityName : Distributions
          startDate : 2014-01-01
          endDate : 2014-04-10
          indicators :
            $a : 1.0
            $b : 2.0
        geometry :
          type : Point
          coordinates :
           -  0.0
           -  0.0
    """

  @AI-574
  Scenario: Querying site's points on public database with authenticated user
    When I request /resources/sites/points?activity=$Distributions
    Then the response should be:
    """
    type : FeatureCollection
    features :
     -
        type : Feature
        id : $SiteId
        properties :
          locationName : Rdc
          partnerName : $NRC
          activity : $Distributions
          activityName : Distributions
          startDate : 2014-01-01
          endDate : 2014-04-10
          indicators :
            $a : 1.0
            $b : 2.0
        geometry :
          type : Point
          coordinates :
           -  0.0
           -  0.0
    """