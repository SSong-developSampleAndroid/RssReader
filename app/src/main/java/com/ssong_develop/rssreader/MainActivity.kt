package com.ssong_develop.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ssong_develop.rssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val dispatcher = newFixedThreadPoolContext(2,"IO")
    private val factory = DocumentBuilderFactory.newInstance()

    private val feeds = listOf(
        "https://www.npr.org/rss/rss.php?id=1001",
        "http://rss.cnn.com/rss/cnn_topstories.rss",
        "http://feeds.foxnews.com/foxnews/politics?format=xml"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        GlobalScope.launch(dispatcher) {
            asyncLoadNews()
        }
    }

    private fun asyncLoadNews() = GlobalScope.launch {
        val requests = mutableListOf<Deferred<List<String>>>()
        feeds.mapTo(requests){
            asyncFetchHeadlines(it,dispatcher)
        }

        requests.forEach {
            it.await()
        }

        val headlines = requests.flatMap {
            it.getCompleted()
        }

        GlobalScope.launch(Dispatchers.Main) {
            binding.tv.text = "Foundd ${headlines.size} news in ${requests.size} feeds"
        }
    }

    private fun asyncFetchHeadlines(feed: String,dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher) {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()

    }
}