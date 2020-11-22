#@namespace scala xed.dictionary.domain.thrift

struct TTermsQuery {
    1: required string field
    2: required list<string> values
}

struct TRangeQuery {
    1: required string field
    2: optional i64 lowValue
    3: required bool lowIncluded
    4: optional i64 highValue
    5: required bool highIncluded
}

struct TMatchQuery {
    1: required string field
    2: required string value
    3: optional string defaultOperator
    4: optional bool analyzeWildcard
    5: optional bool allowLeadingWildcard
}

struct TSortQuery {
    1: required string field
    2: required string order
}

struct TSearchRequest {
    1: optional list<TTermsQuery> terms
    2: optional list<TRangeQuery> ranges
    3: optional list<TMatchQuery> matches
    4: optional list<TSortQuery> sorts
    5: required i32 from
    6: required i32 size

}

#######################

struct TDictionary {
    1: required string word
    2: required string sourceLang
    3: required string targetLang
    4: optional list<string> partOfSpeech
    5: optional i32 dictionaryVersion
    6: optional string data
    8: optional string creator
    9: optional i64 updatedTime
    10: optional i64 createdTime
}

struct TDictionaryResponse
{
    1:required bool success
    2:optional TDictionary dictionary
}

struct TDictionaryListResponse
{
    1: required bool success
    2: optional list<TDictionary> records
    3: optional i64 total
}

struct TDictionaryMapResponse
{
    1: required bool success
    2: optional map<string,TDictionary> records
}