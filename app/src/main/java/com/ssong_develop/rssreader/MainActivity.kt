package com.ssong_develop.rssreader

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ssong_develop.rssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val dispatcher = newSingleThreadContext("ServiceCall")
    private val ioDispatcher = newFixedThreadPoolContext(2, "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        CoroutineScope(dispatcher).launch {
            asyncLoadNews1()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadNews() {
        val headLines = fetchRssHeadlines()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            binding.tv.text = "Found ${headLines.size} News"
        }
    }

    private fun asyncLoadNews() = CoroutineScope(Dispatchers.Main).launch {
        val requests = mutableListOf<Deferred<List<String>>>()

        feeds.mapTo(requests) { asyncFetchHeadlines(it, ioDispatcher) }

        requests.forEach { it.await() }

        val headlines = requests.flatMap { it.getCompleted() }

        binding.tv.text = "Found ${headlines.size} News"
    }

    /**
     * await 대신 join을 사용하면 예외가 전파되지 않는다.
     *
     * 디퍼드가 실패하지 않았을 때만 getCompleted가 호출되도록 변경한다면 flatMap 하는 구간에서도 예외가 전파되지 않을 것이다.
     *
     */
    private fun asyncLoadNews1() = CoroutineScope(Dispatchers.Main).launch {
        val requests = mutableListOf<Deferred<List<String>>>()

        feeds.mapTo(requests) { asyncFetchHeadlines(it, ioDispatcher) }

        requests.forEach { it.join() }

        val headlines = requests.filter { !it.isCancelled }.flatMap { it.getCompleted() }

        binding.tv.text = "Found ${headlines.size} News"
    }

    /**
     * 취소 또는 처리되지 않은 예외로 인해 실행이 종료된 잡은 취소됨(cancelled)로 간주된다.
     *
     * Job이 취소되면 getCancellationException() 함수를 통해 취소에 대한 정보를 얻을 수 있다.
     * getCancellationException()의 반환값을 통해 취소 원인 등의 정보를 검색할 때 사용 가능
     *
     *
     */
    private fun asyncLoadNews2() = CoroutineScope(Dispatchers.Main).launch {
        val requests = mutableListOf<Deferred<List<String>>>()

        feeds.mapTo(requests) { asyncFetchHeadlines(it, ioDispatcher) }

        requests.forEach { it.await() }

        val headlines = requests.flatMap { it.getCompleted() }

        // isCancelled로 잡아도 된다.
        val failed = requests.filter { it.isCancelled }.size

        binding.tv.text = "Found ${headlines.size} News"
    }

    private fun fetchRssHeadlines(): List<String> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")
        val news = xml.getElementsByTagName("channel").item(0)

        return (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()
    }

    private fun asyncFetchHeadlines(
        feed: String,
        dispatcher: CoroutineDispatcher
    ) = CoroutineScope(dispatcher).async {
        val builder = factory.newDocumentBuilder()
        val xml = withContext(Dispatchers.IO) { builder.parse(feed) }
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { it.tagName == "item" }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()
    }

    companion object {
        private val feeds = listOf<String>(
            "https://www.npr.org/rss/rss.php?id=1001",
            "http://rss.cnn.com/rss/cnn_topstories.rss",
            "http://feeds.foxnews.com/foxnews/politics?format=xml"
        )
    }
}