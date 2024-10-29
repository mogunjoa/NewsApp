package com.mogun.newsapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.mogun.newsapp.adapter.NewsAdapter
import com.mogun.newsapp.databinding.ActivityMainBinding
import com.mogun.newsapp.model.NewsRss
import com.mogun.newsapp.model.transform
import com.mogun.newsapp.service.NewsService
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://news.google.com")
        .addConverterFactory(
            TikXmlConverterFactory.create(
                TikXml.Builder()
                    .exceptionOnUnreadXml(false)
                    .build()
            )
        ).build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val newsService = retrofit.create(NewsService::class.java)

        newsAdapter = NewsAdapter(onClick = { url ->
            startActivity(
                Intent(this, WebViewActivity::class.java).apply {
                    putExtra("url", url)
                }
            )
        })
        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
        }

        // 피드
        binding.feedChip.setOnClickListener {
            binding.chipGroup.clearCheck()  // 칩 그룹 check 해제
            binding.feedChip.isChecked = true

            newsService.mainFeed().submitList()
        }

        // 정치
        binding.politicsChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.politicsChip.isChecked = true

            newsService.politicsNews().submitList()
        }

        // 경제
        binding.economyChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.economyChip.isChecked = true

            newsService.economyNews().submitList()
        }

        // 사회
        binding.socialChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.socialChip.isChecked = true

            newsService.societyNews().submitList()
        }

        // 정보기술
        binding.itChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.itChip.isChecked = true

            newsService.itNews().submitList()
        }

        // 스포츠
        binding.sportChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.sportChip.isChecked = true

            newsService.sportNews().submitList()
        }

        // 특정 키패드 버튼 클릭 이벤트
        binding.searchTextInputEditText.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.chipGroup.clearCheck()
                binding.searchTextInputEditText.clearFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                newsService.search(binding.searchTextInputEditText.text.toString()).submitList()

                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        newsService.mainFeed().submitList()
    }

    private fun Call<NewsRss>.submitList() {
        enqueue(object : Callback<NewsRss> {
            override fun onResponse(call: Call<NewsRss>, response: Response<NewsRss>) {
                Log.e("MainActivity", "onResponse: ${response.body()?.channel?.items}")

                val list = response.body()?.channel?.items.orEmpty().transform()
                newsAdapter.submitList(list)

                binding.notFoundView.isVisible = list.isEmpty()

                list.forEachIndexed { index, news ->
                    Thread {
                        try {
                            val jsoup = Jsoup.connect(news.link).get()
                            val elements = jsoup.select("meta[property^=og:]")
                            val ogImageNode = elements.find { node ->
                                node.attr("property") == "og:image"
                            }

                            news.imageUrl = ogImageNode?.attr("content")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        runOnUiThread {
                            newsAdapter.notifyItemChanged(index)
                        }
                    }.start()
                }
            }

            override fun onFailure(call: Call<NewsRss>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}
