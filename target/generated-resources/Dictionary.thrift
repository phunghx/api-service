#@namespace scala xed.dictionary.service
include "DictionaryDT.thrift"


service TDictionaryService {
    string ping()

    DictionaryDT.TDictionaryResponse lookup(1:string sourceLang, 2: string targetLang, 3: string word)
    DictionaryDT.TDictionaryMapResponse multiLookup(1:string sourceLang, 2: string targetLang, 3: list<string> words)
    DictionaryDT.TDictionaryListResponse search(1:string sourceLang, 2: string targetLang, 3: DictionaryDT.TSearchRequest searchRequest)
}

