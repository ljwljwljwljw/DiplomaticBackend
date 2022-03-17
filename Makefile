init:
	git submodule update --init
	cd rocket-chip && git submodule update --init hardfloat api-config-chipsalliance

idea:
	mill mill.scalalib.GenIdea/idea
