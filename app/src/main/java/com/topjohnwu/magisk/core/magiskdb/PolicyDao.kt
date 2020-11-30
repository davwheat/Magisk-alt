package com.topjohnwu.magisk.core.magiskdb

import android.content.Context
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.core.model.su.toMap
import com.topjohnwu.magisk.core.model.su.toPolicy
import com.topjohnwu.magisk.ktx.now
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PolicyDao(
    private val context: Context
) : BaseDao() {

    override val table: String = Table.POLICY

    suspend fun deleteOutdated() = buildQuery<Delete> {
        condition {
            greaterThan("until", "0")
            and {
                lessThan("until", TimeUnit.MILLISECONDS.toSeconds(now).toString())
            }
            or {
                lessThan("until", "0")
            }
        }
    }.commit()

    suspend fun delete(packageName: String) = buildQuery<Delete> {
        condition {
            equals("package_name", packageName)
        }
    }.commit()

    suspend fun delete(uid: Int) = buildQuery<Delete> {
        condition {
            equals("uid", uid)
        }
    }.commit()

    suspend fun fetch(uid: Int) = buildQuery<Select> {
        condition {
            equals("uid", uid)
        }
    }.query().first().toPolicyOrNull()

    suspend fun update(policy: SuPolicy) = buildQuery<Replace> {
        values(policy.toMap())
    }.commit()

    suspend fun <R : Any> fetchAll(mapper: (SuPolicy) -> R) = buildQuery<Select> {
        condition {
            equals("uid/100000", Const.USER_ID)
        }
    }.query {
        it.toPolicyOrNull()?.let(mapper)
    }

    private fun Map<String, String>.toPolicyOrNull(): SuPolicy? {
        return runCatching { toPolicy(context.packageManager) }.getOrElse {
            Timber.w(it)
            val uid = getOrElse("uid") { return null }
            GlobalScope.launch { delete(uid.toInt()) }
            null
        }
    }

}
