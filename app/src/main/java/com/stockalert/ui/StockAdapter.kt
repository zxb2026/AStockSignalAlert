package com.stockalert.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stockalert.R
import com.stockalert.databinding.ItemStockBinding
import java.util.Locale

class StockAdapter(
    private var items: List<StockResult> = emptyList()
) : RecyclerView.Adapter<StockAdapter.VH>() {

    fun submit(list: List<StockResult>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemStockBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val q = r.quote
        val pre = if (q.market == 1) "SH" else "SZ"
        holder.b.txtName.text = q.name
        holder.b.txtCode.text = "$pre.${q.code}"
        holder.b.txtSector.text = q.sector.ifEmpty { "A股" }
        holder.b.txtPrice.text = String.format(Locale.US, "%.2f", q.price)

        val pct = q.changePct
        val up = pct >= 0
        val color = holder.b.txtPrice.context.getColor(
            if (up) R.color.stock_up else R.color.stock_down
        )
        holder.b.txtPrice.setTextColor(color)
        holder.b.txtChangePct.setTextColor(color)
        holder.b.txtChangePct.text = String.format(Locale.US, "%+.2f%%", pct)

        holder.b.txtSignals.text = r.signals.joinToString("  ") { "[$it]" }
    }
}
