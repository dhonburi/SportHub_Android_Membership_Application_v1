package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberTransactionResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionsFragment extends Fragment {

    private ProgressBar progressTransactions;
    private TextView txtTransactionsMessage;
    private Button btnRetryTransactions;
    private LinearLayout layoutTransactionGroups;

    private Call<List<MemberTransactionResponse>> transactionsCall;

    public TransactionsFragment() {
        super(R.layout.fragment_transactions);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        progressTransactions =
                view.findViewById(R.id.progressTransactions);

        txtTransactionsMessage =
                view.findViewById(R.id.txtTransactionsMessage);

        btnRetryTransactions =
                view.findViewById(R.id.btnRetryTransactions);

        layoutTransactionGroups =
                view.findViewById(R.id.layoutTransactionGroups);

        view.findViewById(R.id.btnTransactionsBack)
                .setOnClickListener(backView ->
                        getParentFragmentManager()
                                .popBackStack()
                );

        btnRetryTransactions.setOnClickListener(
                retryView -> loadTransactions()
        );

        loadTransactions();
    }

    private void loadTransactions() {
        int memberId =
                new MemberSession(requireContext())
                        .getMemberId();

        if (memberId <= 0) {
            showError(
                    "No logged-in member was found."
            );
            return;
        }

        showLoading();

        transactionsCall =
                ApiClient
                        .getMemberApiService()
                        .getMemberTransactions(memberId);

        transactionsCall.enqueue(
                new Callback<List<MemberTransactionResponse>>() {

                    @Override
                    public void onResponse(
                            Call<List<MemberTransactionResponse>> call,
                            Response<List<MemberTransactionResponse>> response
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        List<MemberTransactionResponse> transactions =
                                response.body();

                        if (response.isSuccessful()
                                && transactions != null) {

                            if (transactions.isEmpty()) {
                                showEmptyState();
                            } else {
                                displayTransactions(transactions);
                            }

                            return;
                        }

                        if (response.code() == 404) {
                            showError(
                                    "Member profile was not found."
                            );
                            return;
                        }

                        showError(
                                "Unable to load transactions."
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<MemberTransactionResponse>> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        showError(
                                "Unable to connect to the API."
                        );
                    }
                }
        );
    }

    private void displayTransactions(
            List<MemberTransactionResponse> transactions
    ) {
        LinkedHashMap<String, List<MemberTransactionResponse>>
                groupedTransactions = new LinkedHashMap<>();

        for (MemberTransactionResponse transaction : transactions) {
            String dateLabel = formatDate(
                    transaction.getOccurredAtUtc()
            );

            if (!groupedTransactions.containsKey(dateLabel)) {
                groupedTransactions.put(
                        dateLabel,
                        new ArrayList<>()
                );
            }

            List<MemberTransactionResponse> dateTransactions =
                    groupedTransactions.get(dateLabel);

            if (dateTransactions != null) {
                dateTransactions.add(transaction);
            }
        }

        layoutTransactionGroups.removeAllViews();

        LayoutInflater inflater =
                LayoutInflater.from(requireContext());

        for (Map.Entry<String, List<MemberTransactionResponse>> entry
                : groupedTransactions.entrySet()) {

            View groupView = inflater.inflate(
                    R.layout.item_transaction_group,
                    layoutTransactionGroups,
                    false
            );

            TextView txtTransactionDate =
                    groupView.findViewById(
                            R.id.txtTransactionDate
                    );

            LinearLayout layoutTransactionRows =
                    groupView.findViewById(
                            R.id.layoutTransactionRows
                    );

            txtTransactionDate.setText(entry.getKey());

            List<MemberTransactionResponse> dateTransactions =
                    entry.getValue();

            for (int index = 0;
                 index < dateTransactions.size();
                 index++) {

                MemberTransactionResponse transaction =
                        dateTransactions.get(index);

                View rowView = inflater.inflate(
                        R.layout.item_transaction_row,
                        layoutTransactionRows,
                        false
                );

                bindTransactionRow(
                        rowView,
                        transaction
                );

                layoutTransactionRows.addView(rowView);

                if (index < dateTransactions.size() - 1) {
                    View divider = new View(requireContext());

                    LinearLayout.LayoutParams dividerParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    dpToPx(1)
                            );

                    dividerParams.setMarginStart(dpToPx(18));
                    dividerParams.setMarginEnd(dpToPx(18));

                    divider.setLayoutParams(dividerParams);
                    divider.setBackgroundColor(
                            Color.parseColor("#E4E4E4")
                    );

                    layoutTransactionRows.addView(divider);
                }
            }

            layoutTransactionGroups.addView(groupView);
        }

        progressTransactions.setVisibility(View.GONE);
        txtTransactionsMessage.setVisibility(View.GONE);
        btnRetryTransactions.setVisibility(View.GONE);
        layoutTransactionGroups.setVisibility(View.VISIBLE);
    }

    private void bindTransactionRow(
            View rowView,
            MemberTransactionResponse transaction
    ) {
        TextView txtTransactionDescription =
                rowView.findViewById(
                        R.id.txtTransactionDescription
                );

        TextView txtTransactionAmount =
                rowView.findViewById(
                        R.id.txtTransactionAmount
                );

        String description =
                clean(transaction.getDescription());

        if (description.isEmpty()) {
            description = "Transaction";
        }

        String currency = clean(transaction.getCurrency());

        if (currency.isEmpty()) {
            currency = "NZD";
        }

        double amount = transaction.getAmount();

        String amountText = String.format(
                Locale.getDefault(),
                amount >= 0
                        ? "+%.2f %s"
                        : "-%.2f %s",
                Math.abs(amount),
                currency
        );

        txtTransactionDescription.setText(description);
        txtTransactionAmount.setText(amountText);

        txtTransactionAmount.setTextColor(
                Color.parseColor(
                        amount >= 0
                                ? "#2DBE60"
                                : "#111111"
                )
        );

        rowView.setContentDescription(
                description + ", " + amountText
        );
    }

    private String formatDate(String occurredAtUtc) {
        String cleanTimestamp = clean(occurredAtUtc);

        if (cleanTimestamp.isEmpty()) {
            return "Date unavailable";
        }

        SimpleDateFormat inputFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.US
                );

        inputFormat.setLenient(false);
        inputFormat.setTimeZone(
                TimeZone.getTimeZone("UTC")
        );

        SimpleDateFormat outputFormat =
                new SimpleDateFormat(
                        "MMMM d, yyyy",
                        Locale.getDefault()
                );

        outputFormat.setTimeZone(
                TimeZone.getTimeZone("Pacific/Auckland")
        );

        try {
            Date transactionDate =
                    inputFormat.parse(cleanTimestamp);

            if (transactionDate == null) {
                return "Date unavailable";
            }

            return outputFormat.format(transactionDate);

        } catch (ParseException exception) {
            return "Date unavailable";
        }
    }

    private void showLoading() {
        layoutTransactionGroups.removeAllViews();
        layoutTransactionGroups.setVisibility(View.GONE);

        txtTransactionsMessage.setText(
                "Loading your transactions..."
        );

        txtTransactionsMessage.setVisibility(View.VISIBLE);
        btnRetryTransactions.setVisibility(View.GONE);
        progressTransactions.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        layoutTransactionGroups.removeAllViews();
        layoutTransactionGroups.setVisibility(View.GONE);

        progressTransactions.setVisibility(View.GONE);
        btnRetryTransactions.setVisibility(View.GONE);

        txtTransactionsMessage.setText(
                "No transactions yet. Your future top-ups and membership purchases will appear here."
        );

        txtTransactionsMessage.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        layoutTransactionGroups.removeAllViews();
        layoutTransactionGroups.setVisibility(View.GONE);

        progressTransactions.setVisibility(View.GONE);

        txtTransactionsMessage.setText(message);
        txtTransactionsMessage.setVisibility(View.VISIBLE);

        btnRetryTransactions.setVisibility(View.VISIBLE);
    }

    private String clean(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    public void onDestroyView() {
        if (transactionsCall != null
                && !transactionsCall.isCanceled()) {

            transactionsCall.cancel();
        }

        super.onDestroyView();
    }
}
