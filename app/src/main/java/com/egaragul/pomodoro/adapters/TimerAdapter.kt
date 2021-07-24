package com.egaragul.pomodoro.adapters

import android.graphics.drawable.AnimationDrawable
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.egaragul.pomodoro.R
import com.egaragul.pomodoro.TimerControlListener
import com.egaragul.pomodoro.data.Timer
import com.egaragul.pomodoro.databinding.ItemTimeLayoutBinding
import com.egaragul.pomodoro.utils.displayTime

class TimerAdapter(
    private val timerControlListener: TimerControlListener
) : ListAdapter<Timer, TimerAdapter.TimerViewHolder>(timerComparator) {

    init {
        setHasStableIds(true)
    }

    companion object {
        const val INTERVAL = 1000L
        const val CHANGE_BUTTON_STATE = "CHANGE_BUTTON_STATE"

        val timerComparator = object : DiffUtil.ItemCallback<Timer>() {
            override fun areItemsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.isStarted == newItem.isStarted && oldItem.currentMs == newItem.currentMs
            }
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<Timer>, currentList: MutableList<Timer>) {
        super.onCurrentListChanged(previousList, currentList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        return TimerViewHolder(
            ItemTimeLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(getItem(holder.adapterPosition))
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.find { it == CHANGE_BUTTON_STATE }?.let {
                holder.checkTimerState(getItem(holder.adapterPosition))
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    inner class TimerViewHolder(private val binding: ItemTimeLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        private var timeCounter: CountDownTimer? = null

        fun bind(timer: Timer) {
            if (timer.isStarted) {
                val sysClock = SystemClock.uptimeMillis()

                val diffMs = sysClock - timer.globalTime
                if (timer.currentMs > diffMs) {
                    if (diffMs > 900L) {
                        timer.currentMs -= diffMs
                    }
                }
            }


            binding.tvTimer.text = timer.currentMs.displayTime()
            binding.vLoader.setPeriod(timer.startTime)

            //If started before
            if (timer.currentMs != timer.startTime && timer.currentMs > 900L) {
                binding.vLoader.setCurrent(timer.currentMs)
            } else {
                binding.vLoader.setCurrent(0)
            }

            binding.btnStartStop.isEnabled = timer.currentMs != 0L

            binding.btnStartStop.text = if (timer.isStarted) {
                itemView.context.getString(R.string.stop)
            } else {
                itemView.context.getString(R.string.start)
            }

            if (timer.isStarted) {
                startTimer(timer)
            } else {
                stopTimer()
            }

            initListeners(timer)
        }

        private fun initListeners(timer: Timer) {
            binding.btnStartStop.setOnClickListener {
                if (timer.isStarted) {
                    timerControlListener.stop(timer.id)
                    this@TimerAdapter.notifyItemRangeChanged(0, itemCount, CHANGE_BUTTON_STATE)
                } else {
                    timerControlListener.start(timer.id)
                    this@TimerAdapter.notifyItemRangeChanged(0, itemCount, CHANGE_BUTTON_STATE)
                }
            }

            binding.btnDelete.setOnClickListener {
                timerControlListener.delete(timer.id)
            }
        }

        fun checkTimerState(timer: Timer) {
            if (timer.isStarted) {
                binding.btnStartStop.text = itemView.context.getString(R.string.stop)
                startTimer(timer)
            } else {
                binding.btnStartStop.text = itemView.context.getString(R.string.start)
                stopTimer()
            }
        }

        private fun startTimer(timer: Timer) {
            timer.globalTime = SystemClock.uptimeMillis()
            timeCounter?.cancel()
            timeCounter = getCountDownTimer(timer = timer)
            timeCounter?.start()

            blinking(true)
        }

        private fun stopTimer() {
            timeCounter?.cancel()
            blinking(false)
        }

        fun removeTimer() {
//            setIsRecyclable(true)
            timeCounter?.cancel()
        }

        private fun getCountDownTimer(timer: Timer): CountDownTimer {
            return object : CountDownTimer(
                timer.currentMs,
                INTERVAL
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    timer.currentMs = millisUntilFinished
                    //
                    timer.globalTime = SystemClock.uptimeMillis()
                    //
                    binding.tvTimer.text = timer.currentMs.displayTime()
                    binding.vLoader.setCurrent(timer.currentMs)
                }

                override fun onFinish() {
                    binding.tvTimer.text = timer.currentMs.displayTime()
                    binding.vLoader.setCurrent(0)

                    binding.btnStartStop.isEnabled = false
                    binding.btnStartStop.text = itemView.context.getString(R.string.start)

                    timer.isStarted = false
                    blinking(false)
                }
            }
        }

        private fun blinking(isShow: Boolean) {
            if (isShow) {
                binding.ivIndicator.visibility = View.VISIBLE
                (binding.ivIndicator.background as? AnimationDrawable)?.start()
            } else {
                binding.ivIndicator.visibility = View.INVISIBLE
                (binding.ivIndicator.background as? AnimationDrawable)?.stop()
            }
        }
    }
}