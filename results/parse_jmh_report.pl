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

# Find columns of interest
$iname = 0;
$iscore = 4;
$imean = 7;
$isource = 8;

open (IN, $input) or die "Failed to open '$input': $!\n";
my $header = readline(IN);

while (<IN>)
{
    chomp;
    s/"//g;
    s/^.*(Repeat|Single)Use_//;
    $bench = $1;
    @cols = split /,/, $_;
    
    $sources{$cols[$isource]} = 1;
    $means{$cols[$imean]} = 1;
    $data{$cols[$isource]}{$bench}{$cols[$imean]}{$cols[$iname]} = $cols[$iscore];
}
close IN;

print "||Source||Use||Mean||Name||Relative Score||\n";
for $source (sort keys %sources)
{
    for $bench (sort keys %{$data{$source}}) 
    {
        for $mean (sort {$a<=>$b} keys %means)
        {
            $first = 0;
            for $name (sort {
                    $data{$source}{$bench}{$mean}{$b} <=> $data{$source}{$bench}{$mean}{$a}
                } keys %{$data{$source}{$bench}{$mean}}) 
            {
                $score = $data{$source}{$bench}{$mean}{$name};
                if ($first) {
                    $rel = $score / $first;
                } else {
                    $rel = 1;
                    $first = $score;
                }
                print "|$source|$bench|$mean|$name|$rel|\n";
            }
        }
    }
}

# Do it again to compare single & repeats use
open (IN, $input) or die "Failed to open '$input': $!\n";
my $header = readline(IN);

my %data;

while (<IN>)
{
    chomp;
    s/"//g;
    s/^.*(Repeat|Single)Use_/$1 /;
    @cols = split /,/, $_;
    
    $score = ($1 eq 'Repeat') ? $cols[$iscore] : $cols[$iscore] * 10;
    $data{$cols[$isource]}{$cols[$imean]}{$cols[$iname]} = $score;
}
close IN;

print "||Source||Mean||Name||Relative Score||\n";
for $source (sort keys %sources)
{
    for $mean (sort {$a<=>$b} keys %means)
    {
        # Do relative to the original
        $first = $data{$source}{$mean}{'Repeat PoissonSampler'};
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


