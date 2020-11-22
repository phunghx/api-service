#@namespace scala xed.api_service.domain.thrift
struct TCard {
    1: required string id
    2: optional string deckId
    3: optional string username
    4: optional i32 cardVersion
    5: optional string design
    6: optional i64 updatedTime
    7: optional i64 createdTime
}

struct TDeck {
    1: required string id
    2: optional string username
    3: optional string name
    4: optional string thumbnail
    5: optional string description
    6: optional string design
    7: optional list<string> cards
    8: optional i32 deckStatus
    9: optional i64 updatedTime
    10: optional i64 createdTime
}


struct TError {
    1: required string reason
    2: required string message
}

struct TResponse
{
  1: required bool success
  2: optional TError error
}