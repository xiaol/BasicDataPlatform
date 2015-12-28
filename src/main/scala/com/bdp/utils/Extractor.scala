package com.bdp.utils

import org.jsoup.nodes.{ Element, Document }

/**
 * Created by zhange on 15/10/27.
 *
 */

trait Extractor {
  def getMetaContentByName(doc: Document, name: String) = {
    doc.select(s"meta[name=$name]").first().attr("content")
  }

  def getMentBySels(doc: Document, sels: List[String]) = {
    var ment: Element = null
    def extract() {
      for (sel <- sels) {
        ment = doc.select(sel).first()
        if (ment != null)
          return
      }
    }
    extract()
    ment
  }
}
