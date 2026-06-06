package com.soltis.mya.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.data.LocalOfferStore
import com.soltis.mya.data.LocalTransactionStore
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.data.P2pOffer
import com.soltis.mya.data.P2pTransaction
import com.soltis.mya.data.UserProfile
import com.soltis.mya.databinding.ActivityAdminBinding
import com.soltis.mya.databinding.ItemAdminOperationBinding
import com.soltis.mya.login.LoginActivity
import com.soltis.mya.wallet.currencySymbol

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var userStore: LocalUserStore
    private lateinit var transactionStore: LocalTransactionStore
    private lateinit var offerStore: LocalOfferStore
    private lateinit var adapter: AdminSectionAdapter

    private enum class Section { DASHBOARD, USERS, OFFERS, OPERATIONS, DISPUTES, PENDING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userStore = LocalUserStore(this)
        if (userStore.getCurrentUser()?.role != LocalUserStore.ROLE_ADMIN) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        transactionStore = LocalTransactionStore(this)
        offerStore = LocalOfferStore(this)
        adapter = AdminSectionAdapter(emptyList(), ::handleRowAction)
        binding.rvAdminSection.layoutManager = LinearLayoutManager(this)
        binding.rvAdminSection.adapter = adapter

        binding.btnAdminLogout.setOnClickListener { logout() }
        binding.btnAdminBack.setOnClickListener { showDashboard() }
        binding.cardAdminUsers.setOnClickListener { showUsers() }
        binding.cardAdminOffers.setOnClickListener { showOffers() }
        binding.cardAdminOperations.setOnClickListener { showOperations() }
        binding.cardAdminDisputes.setOnClickListener { showDisputes() }
        binding.cardAdminPending.setOnClickListener { showPending() }

        showDashboard()
    }

    private fun showDashboard() {
        val users = userStore.getAllUsers()
        val offers = offerStore.getAllOffers()
        val transactions = transactionStore.getAll()
        val disputes = transactions.filter { it.status == P2pTransaction.STATUS_DISPUTE }
        val pending = transactions.filter { it.isPending() }

        binding.tvAdminSummary.text = "Administrador: ${userStore.getCurrentUser()?.username.orEmpty()}"
        binding.tvAdminUsersCount.text = users.size.toString()
        binding.tvAdminOffersCount.text = offers.size.toString()
        binding.tvAdminOperationsCount.text = transactions.size.toString()
        binding.tvAdminDisputesCount.text = disputes.size.toString()
        binding.tvAdminPendingCount.text = pending.size.toString()

        setSection(Section.DASHBOARD, "Panel principal", emptyList())
    }

    private fun showUsers() {
        val rows = userStore.getAllUsers().map { it.toAdminRow() }
        setSection(Section.USERS, "Usuarios registrados", rows)
    }

    private fun showOffers() {
        val rows = offerStore.getAllOffers().map { it.toAdminRow() }
        setSection(Section.OFFERS, "Ofertas publicadas", rows)
    }

    private fun showOperations() {
        val rows = transactionStore.getAll()
            .sortedByDescending { it.createdAt }
            .map { it.toAdminRow(canResolve = false) }
        setSection(Section.OPERATIONS, "Operaciones", rows)
    }

    private fun showDisputes() {
        val rows = transactionStore.getAll()
            .filter { it.status == P2pTransaction.STATUS_DISPUTE }
            .sortedByDescending { it.createdAt }
            .map { it.toAdminRow(canResolve = true) }
        setSection(Section.DISPUTES, "Disputas", rows)
    }

    private fun showPending() {
        val rows = transactionStore.getAll()
            .filter { it.isPending() }
            .sortedByDescending { it.createdAt }
            .map { it.toAdminRow(canResolve = false) }
        setSection(Section.PENDING, "Operaciones pendientes", rows)
    }

    private fun setSection(section: Section, title: String, rows: List<AdminRow>) {
        val isDashboard = section == Section.DASHBOARD
        binding.adminDashboard.visibility = if (isDashboard) View.VISIBLE else View.GONE
        binding.adminSectionContainer.visibility = if (isDashboard) View.GONE else View.VISIBLE
        binding.btnAdminBack.visibility = if (isDashboard) View.GONE else View.VISIBLE
        binding.tvAdminSectionTitle.text = title
        binding.tvAdminEmpty.visibility = if (!isDashboard && rows.isEmpty()) View.VISIBLE else View.GONE
        binding.tvAdminEmpty.text = emptyText(section)
        adapter.update(rows)
    }

    private fun emptyText(section: Section): String {
        return when (section) {
            Section.USERS -> "No hay usuarios registrados"
            Section.OFFERS -> "No hay ofertas registradas"
            Section.OPERATIONS -> "No hay operaciones registradas"
            Section.DISPUTES -> "No hay disputas abiertas"
            Section.PENDING -> "No hay operaciones pendientes"
            Section.DASHBOARD -> ""
        }
    }

    private fun handleRowAction(row: AdminRow) {
        val tx = row.transaction ?: return
        if (row.action != AdminAction.RESOLVE_DISPUTE) {
            Toast.makeText(this, "Esta seccion es solo de consulta", Toast.LENGTH_SHORT).show()
            return
        }
        showResolveDialog(tx)
    }

    private fun showResolveDialog(tx: P2pTransaction) {
        val sellerEmail = if (tx.operationType == "VENTA") tx.makerEmail else tx.takerEmail
        val buyerEmail = if (tx.operationType == "VENTA") tx.takerEmail else tx.makerEmail

        AlertDialog.Builder(this)
            .setTitle("Resolver disputa ${tx.id}")
            .setMessage(
                "Comprador: $buyerEmail\n" +
                    "Vendedor: $sellerEmail\n" +
                    "Metodo: ${tx.paymentMethod}\n" +
                    "Monto: ${currencySymbol(tx.fromCurrency)} ${"%.2f".format(tx.amount)} ${tx.fromCurrency}\n" +
                    "Total: ${currencySymbol(tx.toCurrency)} ${"%.2f".format(tx.total)} ${tx.toCurrency}\n" +
                    "Motivo: ${tx.disputeReason.ifBlank { "Pago no recibido o divisa no liberada" }}\n\n" +
                    "Elige la resolucion simulada."
            )
            .setPositiveButton("Liberar comprador") { _, _ -> resolveForBuyer(tx, sellerEmail, buyerEmail) }
            .setNegativeButton("Devolver vendedor") { _, _ -> resolveForSeller(tx, sellerEmail) }
            .setNeutralButton("Cerrar disputa") { _, _ -> closeDispute(tx) }
            .show()
    }

    private fun resolveForBuyer(tx: P2pTransaction, sellerEmail: String, buyerEmail: String) {
        val ok = userStore.transferHeldToBuyer(sellerEmail, buyerEmail, tx.fromCurrency, tx.amount)
        if (!ok) {
            Toast.makeText(this, "No se pudo liberar: revisa saldo retenido o usuarios", Toast.LENGTH_LONG).show()
            return
        }

        transactionStore.updateStatus(
            tx.id,
            P2pTransaction.STATUS_COMPLETED,
            disputeReason = "Admin resolvio a favor del comprador"
        )
        Toast.makeText(this, "Divisa liberada al comprador", Toast.LENGTH_SHORT).show()
        showDisputes()
        showDashboardCountersOnly()
    }

    private fun resolveForSeller(tx: P2pTransaction, sellerEmail: String) {
        val ok = userStore.returnHeldToSeller(sellerEmail, tx.fromCurrency, tx.amount)
        if (!ok) {
            Toast.makeText(this, "No se pudo devolver: el vendedor no tiene saldo retenido suficiente", Toast.LENGTH_LONG).show()
            return
        }

        transactionStore.updateStatus(
            tx.id,
            P2pTransaction.STATUS_CANCELLED,
            disputeReason = "Admin resolvio a favor del vendedor"
        )
        Toast.makeText(this, "Saldo retenido devuelto al vendedor", Toast.LENGTH_SHORT).show()
        showDisputes()
        showDashboardCountersOnly()
    }

    private fun closeDispute(tx: P2pTransaction) {
        transactionStore.updateStatus(
            tx.id,
            P2pTransaction.STATUS_CANCELLED,
            disputeReason = "Admin cerro la disputa sin movimiento de saldos"
        )
        Toast.makeText(this, "Disputa cerrada", Toast.LENGTH_SHORT).show()
        showDisputes()
        showDashboardCountersOnly()
    }

    private fun showDashboardCountersOnly() {
        val transactions = transactionStore.getAll()
        binding.tvAdminOperationsCount.text = transactions.size.toString()
        binding.tvAdminDisputesCount.text = transactions.count { it.status == P2pTransaction.STATUS_DISPUTE }.toString()
        binding.tvAdminPendingCount.text = transactions.count { it.isPending() }.toString()
    }

    private fun logout() {
        userStore.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

private enum class AdminAction { NONE, RESOLVE_DISPUTE }

private data class AdminRow(
    val id: String,
    val status: String,
    val title: String,
    val detail: String,
    val actionLabel: String = "",
    val action: AdminAction = AdminAction.NONE,
    val transaction: P2pTransaction? = null
)

private class AdminSectionAdapter(
    private var items: List<AdminRow>,
    private val onAction: (AdminRow) -> Unit
) : RecyclerView.Adapter<AdminSectionAdapter.VH>() {

    class VH(val binding: ItemAdminOperationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminOperationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        with(holder.binding) {
            tvAdminOperationId.text = row.id
            tvAdminOperationStatus.text = row.status
            tvAdminOperationTitle.text = row.title
            tvAdminOperationDetail.text = row.detail
            btnResolveDispute.visibility = if (row.action == AdminAction.NONE) View.GONE else View.VISIBLE
            btnResolveDispute.text = row.actionLabel
            btnResolveDispute.setOnClickListener { onAction(row) }
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<AdminRow>) {
        items = newItems
        notifyDataSetChanged()
    }
}

private fun UserProfile.toAdminRow(): AdminRow {
    val paymentCount = bankCards.size + if (yape.isBlank()) 0 else 1 + if (plin.isBlank()) 0 else 1
    return AdminRow(
        id = username.ifBlank { email },
        status = role,
        title = fullName.ifBlank { "Nombre pendiente" },
        detail = "Correo: $email\nMedios de pago: $paymentCount | Telefono: ${phone.ifBlank { "Pendiente" }}"
    )
}

private fun P2pOffer.toAdminRow(): AdminRow {
    return AdminRow(
        id = id,
        status = status,
        title = "$type $fromCurrency/$toCurrency - ${"%.2f".format(amount)} $fromCurrency",
        detail = "Dueno: $ownerName <$ownerEmail>\nTasa: ${"%.4f".format(rate)} | Metodos: ${paymentMethods.joinToString(", ")}"
    )
}

private fun P2pTransaction.toAdminRow(canResolve: Boolean): AdminRow {
    return AdminRow(
        id = id,
        status = status,
        title = "$operationType $fromCurrency/$toCurrency - ${"%.2f".format(amount)} $fromCurrency",
        detail = "Metodo: $paymentMethod | Total: ${currencySymbol(toCurrency)} ${"%.2f".format(total)} $toCurrency\n" +
            "Comprador: ${buyerEmail()}\nVendedor: ${sellerEmail()}\nMotivo: ${disputeReason.ifBlank { "Sin motivo registrado" }}",
        actionLabel = if (canResolve) "Resolver disputa" else "",
        action = if (canResolve) AdminAction.RESOLVE_DISPUTE else AdminAction.NONE,
        transaction = this
    )
}

private fun P2pTransaction.isPending(): Boolean {
    return status == P2pTransaction.STATUS_PENDING_PROOF || status == P2pTransaction.STATUS_PENDING_CONFIRMATION
}

private fun P2pTransaction.buyerEmail(): String = if (operationType == "VENTA") takerEmail else makerEmail

private fun P2pTransaction.sellerEmail(): String = if (operationType == "VENTA") makerEmail else takerEmail
