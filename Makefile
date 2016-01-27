JAVA=java
JVMOPTS=-Xmx4g
CLI=$(JAVA) $(JVMOPTS) -cp target/patent-searchengine-1.0-SNAPSHOT-jar-with-dependencies.jar SearchEngine.SearchEngineCli
DATA_DIR=data
TMP_DIR=tmp
OUT_DIR=index

INPUT_FILES=$(shell find $(DATA_DIR)/ -name "*.xml")
INTERMEDIATE_FILES=$(INPUT_FILES:$(DATA_DIR)/%=$(TMP_DIR)/%)

$(TMP_DIR)/%/inverted.index: $(DATA_DIR)/%
	mkdir -p $(@D)
	$(CLI) index $(DATA_DIR) $< $(@D)/inverted.index $(@D)/document.index $(@D)/link.index

$(TMP_DIR)/%/document.index: $(TMP_DIR)/%/inverted.index
$(TMP_DIR)/%/link.index: $(TMP_DIR)/%/inverted.index

$(OUT_DIR)/inverted.index: $(INTERMEDIATE_FILES:%=%/inverted.index)
	mkdir -p $(@D)
	$(CLI) merge-inv $(INTERMEDIATE_FILES:%=%/inverted.index) $@

$(OUT_DIR)/document.index: $(INTERMEDIATE_FILES:%=%/document.index)
	mkdir -p $(@D)
	$(CLI) merge-doc $(DATA_DIR) $(INTERMEDIATE_FILES:%=%/document.index) $@

$(OUT_DIR)/link.index: $(INTERMEDIATE_FILES:%=%/link.index)
	mkdir -p $(@D)
	$(CLI) merge-link $(INTERMEDIATE_FILES:%=%/link.index) $@


all: $(OUT_DIR)/inverted.index $(OUT_DIR)/document.index $(OUT_DIR)/link.index


index_test/inverted.index index_test/document.index index_test/link.index: data_test/testData.xml
	mkdir -p $(@D)
	$(CLI) index data_test $< index_test/inverted.index index_test/document.index index_test/link.index

test: index_test/inverted.index index_test/document.index index_test/link.index
