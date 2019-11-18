package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import com.stripe.android.R
import java.util.Locale

internal class CountryAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val countryAutocomplete: AutoCompleteTextView

    /**
     * @return 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    var selectedCountry: Country

    @JvmSynthetic
    internal var countryChangeCallback: (Country) -> Unit = {}

    init {
        View.inflate(getContext(), R.layout.country_autocomplete_textview, this)

        val countryAdapter = CountryAdapter(
            getContext(),
            CountryUtils.getOrderedCountries(
                ConfigurationCompat.getLocales(context.resources.configuration)[0]
            )
        )

        countryAutocomplete = findViewById(R.id.autocomplete_country_cat)
        countryAutocomplete.threshold = 0
        countryAutocomplete.setAdapter(countryAdapter)
        countryAutocomplete.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                updatedSelectedCountryCode(countryAdapter.getItem(position))
            }
        countryAutocomplete.onFocusChangeListener = OnFocusChangeListener { _, focused ->
            if (focused) {
                countryAutocomplete.showDropDown()
            } else {
                val countryEntered = countryAutocomplete.text.toString()
                updateUiForCountryEntered(countryEntered)
            }
        }

        val initialCountry = countryAdapter.getItem(0)
        countryAutocomplete.setText(initialCountry.name)
        selectedCountry = initialCountry
        countryChangeCallback(initialCountry)
    }

    /**
     * @param countryCode specify a country code to display in the input. The input will display
     * the full country display name.
     */
    internal fun setCountrySelected(countryCode: String) {
        updateUiForCountryEntered(getDisplayCountry(countryCode))
    }

    @VisibleForTesting
    internal fun updateUiForCountryEntered(displayCountryEntered: String) {
        val country = CountryUtils.getCountryByName(displayCountryEntered)

        // If the user-typed country matches a valid country, update the selected country
        // Otherwise, revert back to last valid country if country is not recognized.
        val displayCountry = country?.let {
            updatedSelectedCountryCode(it)
            displayCountryEntered
        } ?: selectedCountry.name

        countryAutocomplete.setText(displayCountry)
    }

    private fun updatedSelectedCountryCode(country: Country) {
        if (selectedCountry != country) {
            selectedCountry = country
            countryChangeCallback(country)
        }
    }

    private fun getDisplayCountry(countryCode: String): String {
        return CountryUtils.getCountryByCode(countryCode)?.name
            ?: Locale("", countryCode).displayCountry
    }
}
