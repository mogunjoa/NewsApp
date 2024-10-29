package com.mogun.newsapp.model

data class NewsModel(
    val title: String,
    val link: String,
    var imageUrl: String? = null,
)

// 확장 함수
fun List<NewsItem>.transform(): List<NewsModel> {
    return this.map {
        NewsModel(
            title = it.title ?: "",
            link = it.link ?: "",
            imageUrl = null
        )
    }
}