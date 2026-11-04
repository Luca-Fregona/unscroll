# R8 rules for release builds.
#
# Nothing is needed for the service, the activity and the application class: the manifest rules that AGP
# generates keep them. Compose and kotlinx.serialization ship their own consumer rules. unscroll reads JSON
# through JsonElement and never through reflection, so there is nothing to keep for the rule parser either.
