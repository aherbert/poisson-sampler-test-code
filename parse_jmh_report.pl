#!/usr/bin/perl -w

use Getopt::Long;
use File::Basename;

my $prog = basename($0);

my $usage = "
  Program to parse the CSV output from the JMH benchmark and produce a table
  for insertion into JIRA

Usage:

  $prog input [...]

Options:

  input     The JMH CSV report. This must be in unix file format on Linux.

  -help     Print this help and exit

";

my $help;
GetOptions(
	"help" => \$help,
);

die $usage if $help;

# Delete as necessary
my $input = shift or die $usage;

open (IN, $input) or die "Failed to open '$input': $!\n";
my $header = readline(IN);

chomp($header);
$header =~ s/"//g;
@cols = split /,/, $header;

# Find columns of interest
$iname = 0;
$iscore = 4;
$imean = 7;
$isource = 8;

$max = 0;

while (<IN>)
{
    chomp;
    s/"//g;
    s/^[^_]*_//;
    @cols = split /,/, $_;
    
    $sources{$cols[$isource]} = 1;
    $means{$cols[$imean]} = 1;
    $data{$cols[$isource]}{$cols[$imean]}{$cols[$iname]} = $cols[$iscore];
    $max = $cols[$iscore] if $max < $cols[$iscore];
}
close IN;

print "||Source||Mean||Name||Relative Score||\n";
for $source (sort keys %sources)
{
    for $mean (sort {$a<=>$b} keys %means)
    {
        $first = 0;
        for $name (sort {
                $data{$source}{$mean}{$b} <=> $data{$source}{$mean}{$a}
            } keys %{$data{$source}{$mean}}) 
        {
            $score = $data{$source}{$mean}{$name};
            if ($first) {
                $rel = $score / $first;
            } else {
                $rel = 1;
                $first = $score;
            }
            print "|$source|$mean|$name|$rel|\n";
        }
    }
}