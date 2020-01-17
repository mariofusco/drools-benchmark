/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.benchmarks;

import java.security.ProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.drools.benchmarks.domain.Address;
import org.drools.benchmarks.domain.Person;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@Fork(1)
@State(Scope.Thread)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class EvalBenchmark {

    private static final String EVAL_DRL =
            "import org.drools.benchmarks.domain.Address\n" +
            "import org.drools.benchmarks.domain.Person\n" +
            "rule R1 when\n" +
            "  $p : Person()\n" +
            "  $a : Address()" +
            "  eval( $p.getName() == \"Mario\" && $a.getStreet() == \"Main Street\" && $a == $p.getAddress() )" +
            "then\n" +
            "end\n" +
            "rule R2 when\n" +
            "  $p : Person()\n" +
            "  $a : Address()" +
            "  eval( $p.getName() == \"Duncan\" && $a.getStreet() == \"First Street\" && $a == $p.getAddress() )" +
            "then\n" +
            "end\n" +
            "rule R3 when\n" +
            "  $p : Person()\n" +
            "  $a : Address()" +
            "  eval( $p.getName() == \"Toshiya\" && $a.getStreet() == \"Second Street\" && $a == $p.getAddress() )" +
            "then\n" +
            "end\n";

    private static final String NO_EVAL_DRL =
            "import org.drools.benchmarks.domain.Address\n" +
            "import org.drools.benchmarks.domain.Person\n" +
            "rule R1 when\n" +
            "  $p : Person( name == \"Mario\" )\n" +
            "  $a : Address( street == \"Main Street\", this == $p.address )" +
            "then\n" +
            "end\n" +
            "rule R2 when\n" +
            "  $p : Person( name == \"Duncan\" )\n" +
            "  $a : Address( street == \"First Street\", this == $p.address )" +
            "then\n" +
            "end\n" +
            "rule R3 when\n" +
            "  $p : Person( name == \"Toshiya\" )\n" +
            "  $a : Address( street == \"Second Street\", this == $p.address )" +
            "then\n" +
            "end\n";

    private static final Map<String, String> drls = new HashMap<>();

    static {
        drls.put( "eval", EVAL_DRL );
        drls.put( "no_eval", NO_EVAL_DRL );
    }

    @Param({"eval", "no_eval"})
    private String drl;

    private KieBase kbase;
    private KieSession ksession;

    private final Person[] persons = new Person[1000];
    private final Address[] addresses = new Address[1000];

    @Setup
    public void setup1() {
        prepareData();
        kbase = new KieHelper().addContent( drls.get(drl), ResourceType.DRL ).build();
    }

    @Setup(Level.Iteration)
    public void setup2() throws ProviderException {
        ksession = kbase.newKieSession();
    }

    @Benchmark
    public int run() {
        for (int i = 0; i < persons.length; i++) {
            ksession.insert( persons[i] );
            ksession.insert( addresses[i] );
        }

        int firings = ksession.fireAllRules();
        if (firings != 3) {
            throw new RuntimeException( "Wrong result: " + firings );
        }
        return firings;
    }

    private void prepareData() {
        for (int i = 0; i < persons.length; i++) {
            addresses[i] = new Address( UUID.randomUUID().toString() );
            persons[i] = new Person( UUID.randomUUID().toString() ).setAddress( addresses[i] );
        }

        addresses[250] = new Address( "Main Street" );
        persons[250] = new Person( "Mario" ).setAddress( addresses[250] );

        addresses[500] = new Address( "First Street" );
        persons[500] = new Person( "Duncan" ).setAddress( addresses[500] );

        addresses[750] = new Address( "Second Street" );
        persons[750] = new Person( "Toshiya" ).setAddress( addresses[750] );
    }
}
