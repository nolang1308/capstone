package com.google.mediapipe.examples.handlandmarker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
data class GridItem(
    val imageResId: Int,
    val text1: String,
    val text2: String
)
class GridAdapter(private val context: Context, private val items: List<GridItem>) : BaseAdapter() {
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.item_image)
        val textView1 = view.findViewById<TextView>(R.id.item_text1)
        val textView2 = view.findViewById<TextView>(R.id.item_text2)

        val item = items[position]
        imageView.setImageResource(item.imageResId)
        textView1.text = item.text1
        textView2.text = item.text2

        return view
    }
}