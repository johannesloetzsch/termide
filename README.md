# termide

Terminal IDE for [re-frame](https://github.com/Day8/re-frame-template)/[shadow-cljs](https://github.com/thheller/shadow-cljs) projects

Based on [tmux](https://github.com/tmux/tmux/wiki) and [vim-fireplace](https://github.com/tpope/vim-fireplace)

## Usage

Put `[termide "0.1.0"]` into the `:plugins` vector of your `project.clj`

or into the `:plugins` vector of your `:user` profile of `.lein/profiles.clj`.

## Example

    $ PROJECT=example
    $ lein new re-frame $PROJECT
    $ cd $PROJECT

    $ # echo '{:user {:plugins [[termide "0.1.0"]]}}' > ~/.lein/profiles.clj

    $ lein termide
    $ xdg-open http://localhost:8280
