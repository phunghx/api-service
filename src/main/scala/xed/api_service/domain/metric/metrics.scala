package xed.api_service.domain.metric


case class CountMetric(time: Long,
                       total: Int,
                       hits: Long)

case class Entry(time: Long,
                 value: Long,
                 hits: Long)

case class LineData(label: String,
                    total: Long,
                    records: Seq[Entry])

case class CardReport(newCardCount: Long,
                      learningCardCount: Long,
                      completedCardCount: Long,
                      ignoredCardCount: Long)

