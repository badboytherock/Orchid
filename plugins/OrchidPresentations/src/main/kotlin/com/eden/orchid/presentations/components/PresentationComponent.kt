package com.eden.orchid.presentations.components

import com.eden.orchid.api.options.annotations.BooleanDefault
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.options.annotations.StringDefault
import com.eden.orchid.api.theme.assets.AssetManagerDelegate
import com.eden.orchid.api.theme.components.OrchidComponent
import com.eden.orchid.presentations.model.Presentation

@Description("Embed a Deck.js presentation.", name = "Presentation")
class PresentationComponent : OrchidComponent("presentation") {

    @Option
    @StringDefault("web-2_0")
    @Description("The Deck.js presentation theme to use. Should be one of ['neon', 'swiss', 'web-2_0']")
    lateinit var deckTheme: String

    @Option
    @StringDefault("horizontal-slide")
    @Description("The Deck.js transition theme to use. Should be one of ['fade', 'horizontal-slide', 'vertical-slide']")
    lateinit var transitionTheme: String

    @Option
    @BooleanDefault(false)
    @Description("If true, only include the Deck.js Javascript files, opting to build the styles yourself.")
    var scriptsOnly: Boolean = false

    @Option
    @Description("The key of the Presentation to display.")
    var presentation: Presentation? = null

    override fun loadAssets(delegate: AssetManagerDelegate) {
        if (!scriptsOnly) {
            delegate.addCss("assets/core/deck_core.scss")
            delegate.addCss("assets/extensions/goto/deck_goto.scss")
            delegate.addCss("assets/extensions/menu/deck_menu.scss")
            delegate.addCss("assets/extensions/navigation/deck_navigation.scss")
            delegate.addCss("assets/extensions/status/deck_status.scss")
            delegate.addCss("assets/extensions/scale/deck_scale.scss")
            delegate.addCss("assets/themes/style/$deckTheme.scss")
            delegate.addCss("assets/themes/transition/$transitionTheme.scss")
        }

        delegate.addJs("assets/vendor/modernizr_custom.js")
        delegate.addJs("assets/core/deck_core.js")
        delegate.addJs("assets/extensions/menu/deck_menu.js")
        delegate.addJs("assets/extensions/goto/deck_goto.js")
        delegate.addJs("assets/extensions/status/deck_status.js")
        delegate.addJs("assets/extensions/navigation/deck_navigation.js")
        delegate.addJs("assets/extensions/scale/deck_scale.js")
        delegate.addJs("assets/initDeck.js")
    }

}
