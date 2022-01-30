package com.ssong_develop.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ssong_develop.rssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val dispatcher = newSingleThreadContext("ServiceCall")
    private val factory = DocumentBuilderFactory.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        GlobalScope.launch(dispatcher) {
            loadNews()
        }
    }

    private fun loadNews() {
        val headLines = fetchRssHeadlines()
        GlobalScope.launch(Dispatchers.Main.immediate) {
            binding.tv.text = "Found ${headLines.size} News"
        }
    }

    private fun fetchRssHeadlines(): List<String> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")
        val news = xml.getElementsByTagName("channel").item(0)

        return (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map {it as Element }
            .filter { "item" == it.tagName }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()
    }
}