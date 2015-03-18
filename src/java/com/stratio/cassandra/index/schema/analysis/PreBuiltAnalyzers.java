/*
 * Copyright 2009-2014 Elasticsearch.
 * Modification and adapations - Copyright 2015, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.schema.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.ckb.SoraniAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.eu.BasqueAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;

import java.util.Locale;

/**
 *
 */
public enum PreBuiltAnalyzers {

    STANDARD() {
        @Override
        protected Analyzer create() {
            return new StandardAnalyzer();
        }
    },

    DEFAULT() {
        @Override
        protected Analyzer create() {
            return STANDARD.create();
        }
    },

    KEYWORD() {
        @Override
        protected Analyzer create() {
            return new KeywordAnalyzer();
        }
    },

    STOP {
        @Override
        protected Analyzer create() {
            return new StopAnalyzer();

        }
    },

    WHITESPACE {
        @Override
        protected Analyzer create() {
            return new WhitespaceAnalyzer();

        }
    },

    SIMPLE {
        @Override
        protected Analyzer create() {
            return new SimpleAnalyzer();

        }
    },

    CLASSIC {
        @Override
        protected Analyzer create() {
            return new ClassicAnalyzer();

        }
    },

    ARABIC {
        @Override
        protected Analyzer create() {
            return new ArabicAnalyzer();
        }
    },

    ARMENIAN {
        @Override
        protected Analyzer create() {
            return new ArmenianAnalyzer();
        }
    },

    BASQUE {
        @Override
        protected Analyzer create() {
            return new BasqueAnalyzer();
        }
    },

    BRAZILIAN {
        @Override
        protected Analyzer create() {
            return new BrazilianAnalyzer();
        }
    },

    BULGARIAN {
        @Override
        protected Analyzer create() {
            return new BulgarianAnalyzer();
        }
    },

    CATALAN {
        @Override
        protected Analyzer create() {
            return new CatalanAnalyzer();
        }
    },

    CHINESE() {
        @Override
        protected Analyzer create() {
            return new StandardAnalyzer();
        }
    },

    CJK {
        @Override
        protected Analyzer create() {
            return new CJKAnalyzer();
        }
    },

    CZECH {
        @Override
        protected Analyzer create() {
            return new CzechAnalyzer();
        }
    },

    DUTCH {
        @Override
        protected Analyzer create() {
            return new DutchAnalyzer();

        }
    },

    DANISH {
        @Override
        protected Analyzer create() {
            return new DanishAnalyzer();

        }
    },

    ENGLISH {
        @Override
        protected Analyzer create() {
            return new EnglishAnalyzer();
        }
    },

    FINNISH {
        @Override
        protected Analyzer create() {
            return new FinnishAnalyzer();
        }
    },

    FRENCH {
        @Override
        protected Analyzer create() {
            return new FrenchAnalyzer();
        }
    },

    GALICIAN {
        @Override
        protected Analyzer create() {
            return new GalicianAnalyzer();
        }
    },

    GERMAN {
        @Override
        protected Analyzer create() {
            return new GermanAnalyzer();
        }
    },

    GREEK {
        @Override
        protected Analyzer create() {
            return new GreekAnalyzer();
        }
    },

    HINDI {
        @Override
        protected Analyzer create() {
            return new HindiAnalyzer();

        }
    },

    HUNGARIAN {
        @Override
        protected Analyzer create() {
            return new HungarianAnalyzer();
        }
    },

    INDONESIAN {
        @Override
        protected Analyzer create() {
            return new IndonesianAnalyzer();
        }
    },

    IRISH {
        @Override
        protected Analyzer create() {
            return new IrishAnalyzer();
        }
    },

    ITALIAN {
        @Override
        protected Analyzer create() {
            return new ItalianAnalyzer();
        }
    },

    LATVIAN {
        @Override
        protected Analyzer create() {
            return new LatvianAnalyzer();
        }
    },

    NORWEGIAN {
        @Override
        protected Analyzer create() {
            return new NorwegianAnalyzer();
        }
    },

    PERSIAN {
        @Override
        protected Analyzer create() {
            return new PersianAnalyzer();
        }
    },

    PORTUGUESE {
        @Override
        protected Analyzer create() {
            return new PortugueseAnalyzer();
        }
    },

    ROMANIAN {
        @Override
        protected Analyzer create() {
            return new RomanianAnalyzer();

        }
    },

    RUSSIAN {
        @Override
        protected Analyzer create() {
            return new RussianAnalyzer();
        }
    },

    SORANI {
        @Override
        protected Analyzer create() {
            return new SoraniAnalyzer();
        }
    },

    SPANISH {
        @Override
        protected Analyzer create() {
            return new SpanishAnalyzer();
        }
    },

    SWEDISH {
        @Override
        protected Analyzer create() {
            return new SwedishAnalyzer();
        }
    },

    TURKISH {
        @Override
        protected Analyzer create() {
            return new TurkishAnalyzer();
        }
    },

    THAI {
        @Override
        protected Analyzer create() {
            return new ThaiAnalyzer();
        }
    };

    abstract protected Analyzer create();

    public synchronized static Analyzer get(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT)).create();
        } catch (IllegalArgumentException ie) {
            return null;
        }
    }
}
