<senecaJob xmlns="http://cdk.sf.net/seneca/">
  <data CH0="2" CH1="3" CH2="2" CH3="3" />  
  
  <generator id="org.openscience.cdk.structgen.RandomGenerator" enabled="true"><numberSteps value="1000" /></generator>
  
  
<judge id="net.bioclipse.seneca.judge.HMBCJudge" enabled="true" weight="10" data="2d.cml">
</judge>
<generator id="org.openscience.cdk.structgen.UserConfigurableRandomGenerator" enabled="false"><acceptanceProbability value="0.8" /><coolingRate value="0.95" /><initializationCycles value="200" /><convergenceStopCount value="4500" /><maxPlateauSteps value="1500" /><maxUphillSteps value="150" /></generator><judge id="net.bioclipse.seneca.judge.WCCNMRShiftDBJudge" enabled="true" weight="1" data="c13.cml" /><mf>C10H16</mf><title>Example</title><detectAromaticity>false</detectAromaticity></senecaJob>