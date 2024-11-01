package karm.van.repo.elasticRepo;

import karm.van.model.CardDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDate;

public interface ElasticRepo extends ElasticsearchRepository<CardDocument, Long> {
    @Query("""
            {
                "bool": {
                    "should": [
                        {
                            "multi_match": {
                                "query": "?0",
                                "fields": ["title^4", "text^3"],
                                "type": "best_fields",
                                "operator": "or"
                            }
                        },
                        {
                            "match_phrase": {
                                "title": {
                                    "query": "?0",
                                    "boost": 3
                                }
                            }
                        },
                        {
                            "match_phrase": {
                                "text": {
                                    "query": "?0",
                                    "boost": 2
                                }
                            }
                        }
                    ],
                    "minimum_should_match": 1
                }
            }
            """)
    Page<CardDocument> findByQuery(String query, Pageable pageable);

    @Query("""
       {               
           "bool": {                       
               "should": [                           
                   {                               
                       "multi_match": {                                   
                           "query": "?0",                                   
                           "fields": [                                       
                               "title^4",                                       
                               "text^3"                                   
                           ],                                   
                           "type": "best_fields",                                   
                           "operator": "or"                               
                       }                           
                   },                           
                   {                               
                       "match_phrase": {                                   
                           "title": {                                       
                               "query": "?0",                                       
                               "boost": 3                                   
                           }                               
                       }                           
                   },                           
                   {                               
                       "match_phrase": {                                   
                           "text": {                                       
                               "query": "?0",                                       
                               "boost": 2                                   
                           }                               
                       }                           
                   }                       
               ],                       
               "filter": [                           
                   {                               
                       "range": {                                   
                           "createTime": {                                       
                               "gte": "?1"                                   
                           }                               
                       }                           
                   }                       
               ]                   
           }               
       }
       """)
    Page<CardDocument> findByQueryAndSortByData(String query, String createTime, Pageable pageable);
}
