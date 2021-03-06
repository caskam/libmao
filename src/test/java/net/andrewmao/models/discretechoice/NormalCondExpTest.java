package net.andrewmao.models.discretechoice;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import net.andrewmao.models.noise.NormalLogLikelihood;
import net.andrewmao.models.noise.TestParameterGen;
import net.andrewmao.probability.MultivariateNormal;
import net.andrewmao.probability.MultivariateNormal.CDFResult;
import org.apache.commons.math3.linear.RealVector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests conditional expectation - MVN exp and gibbs sampling
 * @author mao
 *
 */
@RunWith(Parameterized.class)
public class NormalCondExpTest {

	static int trials = 10;	
	static int gibbsSamplesHigh = 100000;
	
	static double tol = 0.04;
	
	RealVector mean, var;
	int[] ranking;
	
	NormalMoments condExpMVN, condExpMVNVar;
	
	MultivariateNormal mvnPrecise = new MultivariateNormal(1<<17, 1e-6, 1e-5, 1e-4, 1e-3, true);
	MultivariateNormal mvn = MultivariateNormal.DEFAULT_INSTANCE;
	
	public NormalCondExpTest(RealVector mean, RealVector var, int[] ranking) {
		this.mean = mean;
		this.var = var;
		this.ranking = ranking;
		
		this.condExpMVN = OrderedNormalEM.conditionalMean(mean, var, ranking, mvnPrecise);
		this.condExpMVNVar = OrderedNormalEM.conditionalMoments(mean, var, ranking, mvnPrecise);
		
		System.out.println();
	}

	@Parameters
	public static Collection<Object[]> tnImpls() {											
		return TestParameterGen.randomMeanVarRankings(4, trials);
	}
	
	@Before
	public void setUp() throws Exception {
				
	}

	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public void testLLEquivalence() {				
		// Ensure that we can use the conditional expectation MVN value to estimate likelihood		
		
		NormalLogLikelihood ll = new NormalLogLikelihood(mean, var);
		CDFResult cdf = ll.multivariateProb(ranking);		
		
		assertEquals(cdf.cdf, condExpMVN.cdf, 1e-4);
		assertEquals(cdf.cdf, condExpMVNVar.cdf, 1e-4);		
	}
	
	@Test
	public void testConditionalExpectationQuick() {
		System.out.println("Testing bias ");
		NormalMoments condExpMVNQuick = OrderedNormalEM.conditionalMean(mean, var, ranking, mvn);		
						
		System.out.println("MVN Quick: " + Arrays.toString(condExpMVNQuick.m1));		
		System.out.println("MVN Accurate: " + Arrays.toString(condExpMVN.m1));
		
		// check rankings are consistent
		assertTrue("MVN Quick conditional expectation is inconsistent", checkConsistency(condExpMVNQuick));
		assertTrue("MVN conditional expectation is inconsistent", checkConsistency(condExpMVN));				
		
		assertArrayEquals(condExpMVN.m1, condExpMVNQuick.m1, tol);		
	}

	@Test
	public void testConditionalExpectationQuickVar() {
		System.out.println("Testing bias with two moments");
		NormalMoments condExpMVNQuick = OrderedNormalEM.conditionalMoments(mean, var, ranking, mvn);		
						
		System.out.println("MVN Quick: " + Arrays.toString(condExpMVNQuick.m1));		
		System.out.println("MVN Accurate: " + Arrays.toString(condExpMVNVar.m1));
		
		// check rankings are consistent
		assertTrue("MVN Quick conditional expectation is inconsistent", checkConsistency(condExpMVNQuick));
		assertTrue("MVN conditional expectation is inconsistent", checkConsistency(condExpMVNVar));				
		
		System.out.println("MVN M2: " + Arrays.toString(condExpMVNVar.m2));		
		System.out.println("MVN Quick M2: " + Arrays.toString(condExpMVNQuick.m2));
		
		assertArrayEquals(condExpMVNVar.m1, condExpMVNQuick.m1, tol);
		fail("Second moment of Integral is not implemented correctly");
		assertArrayEquals(condExpMVNVar.m2, condExpMVNQuick.m2, tol);
	}
	
	@Test
	public void testConditionalExpectationGibbs() {
		System.out.println("Testing Gibbs ");
		NormalMoments condExpGibbs = new NormalGibbsSampler(mean, var, ranking, gibbsSamplesHigh, false).call();
				
		System.out.println("MVN: " + Arrays.toString(condExpMVN.m1));		
		System.out.println("Gibbs: " + Arrays.toString(condExpGibbs.m1));
		
		// check rankings are consistent
		assertTrue("MVN conditional expectation is inconsistent", checkConsistency(condExpMVN));
		assertTrue("Gibbs conditional expectation is inconsistent", checkConsistency(condExpGibbs));				
		
		assertArrayEquals(condExpMVN.m1, condExpGibbs.m1, tol);		
	}
	
	@Test
	public void testConditionalExpectationGibbsVar() {
		System.out.println("Testing Gibbs with variance ");
		NormalMoments condExpGibbs = new NormalGibbsSampler(mean, var, ranking, gibbsSamplesHigh, true).call();
				
		System.out.println("MVN: " + Arrays.toString(condExpMVNVar.m1));		
		System.out.println("Gibbs: " + Arrays.toString(condExpGibbs.m1));
		
		// check rankings are consistent
		assertTrue("MVN conditional expectation is inconsistent", checkConsistency(condExpMVNVar));
		assertTrue("Gibbs conditional expectation is inconsistent", checkConsistency(condExpGibbs));				
		
		System.out.println("MVN M2: " + Arrays.toString(condExpMVNVar.m2));		
		System.out.println("Gibbs M2: " + Arrays.toString(condExpGibbs.m2));
		
		assertArrayEquals(condExpMVNVar.m1, condExpGibbs.m1, tol);
		assertArrayEquals(condExpMVNVar.m2, condExpGibbs.m2, tol);
	}

	private boolean checkConsistency(NormalMoments condExp) {
		for( int i = 1; i < ranking.length; i++ ) {
			// It's wrong if the value(i) > value(i-1)
			if( condExp.m1[ranking[i]-1] > condExp.m1[ranking[i-1]-1] ) {
				return false;
			}
		}
		return true;
	}

}
